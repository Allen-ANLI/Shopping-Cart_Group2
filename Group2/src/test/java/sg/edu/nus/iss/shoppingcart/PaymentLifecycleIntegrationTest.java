package sg.edu.nus.iss.shoppingcart;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import sg.edu.nus.iss.shoppingcart.entity.*;
import sg.edu.nus.iss.shoppingcart.repository.*;
import sg.edu.nus.iss.shoppingcart.service.*;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:payment-lifecycle;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class PaymentLifecycleIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository items;
    @Autowired CartService cart;
    @Autowired CheckoutCoordinator checkout;
    @Autowired ProductReviewService reviews;
    @Autowired CheckoutReceiptService receipts;

    private MockHttpSession customer() {
        var user = users.saveAndFlush(new User("lifecycle_" + UUID.randomUUID().toString().substring(0,8), "hash", "Buyer", null));
        var session = new MockHttpSession(); session.setAttribute("loginUserId", user.getId()); return session;
    }
    private Product product() {
        var product = new Product(); product.setName("Lifecycle " + UUID.randomUUID()); product.setCategory("audio");
        product.setPrice(new BigDecimal("100.00")); product.setDiscountPercent(25); product.setStockQuantity(10);
        return products.saveAndFlush(product);
    }
    private Long buy(MockHttpSession buyer, Product product) {
        cart.addItem(buyer, product.getId(), 2); return checkout.submit(buyer, checkout.prepare(buyer));
    }
    private Long userId(MockHttpSession session) { return (Long) session.getAttribute("loginUserId"); }

    @Test void deliveredOrderUnlocksReviewLinksOnlyAfterOwnerConfirmsAndKeepsSafePaymentSnapshot() throws Exception {
        var buyer = customer(); var product = product(); Long id = buy(buyer, product);
        var order = orders.findById(id).orElseThrow();
        assertThat(order.getShipmentDeliveredAt()).isNotNull(); assertThat(order.isReceiptConfirmed()).isFalse();
        assertThat(order.getTotalAmount()).isEqualByComparingTo("150.00");
        assertThat(items.findByOrder_IdOrderByIdAsc(id).get(0).getUnitPrice()).isEqualByComparingTo("75.00");
        assertThat(order.getPaymentLastDigits()).isEqualTo("4242");
        assertThat(receipts.forUser(order.getCheckoutToken(), userId(buyer)).payment().toString()).doesNotContain("4242424242424242", "123456");
        mvc.perform(get("/orders/{id}", id).session(buyer)).andExpect(status().isOk())
                .andExpect(content().string(containsString("awaiting your confirmation")))
                .andExpect(content().string(not(containsString("class=\"review-action\""))));
        assertThat(reviews.read(product.getId(), userId(buyer)).canReview()).isFalse();
        mvc.perform(post("/orders/{id}/reviews/{product}", id, product.getId()).session(buyer)
                .param("cartFormToken", cart.formToken(buyer)).param("rating", "5").param("comment", "Excellent purchase"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/orders/{id}/confirm", id).session(buyer).param("cartFormToken", cart.formToken(buyer)))
                .andExpect(status().isOk()).andExpect(view().name("orders/detail"))
                .andExpect(content().string(containsString("class=\"review-action\"")));
        var confirmedAt = orders.findById(id).orElseThrow().getReceiptConfirmedAt();
        mvc.perform(post("/orders/{id}/confirm", id).session(buyer).param("cartFormToken", cart.formToken(buyer))).andExpect(status().isOk());
        assertThat(orders.findById(id).orElseThrow().getReceiptConfirmedAt()).isEqualTo(confirmedAt);
        mvc.perform(post("/orders/{id}/reviews/{product}", id, product.getId()).session(buyer)
                .param("cartFormToken", cart.formToken(buyer)).param("rating", "4").param("comment", "Excellent purchase"))
                .andExpect(redirectedUrl("/orders/" + id + "?reviewed"));
        assertThat(reviews.ownReview(product.getId(), userId(buyer)).rating()).isEqualTo(4);
        for(String language:java.util.List.of("en","zh")) {
            mvc.perform(get("/orders/{id}",id).session(buyer).cookie(new Cookie("store_lang",language)))
                    .andExpect(status().isOk()).andExpect(content().string(containsString(language.equals("en")?"Order products":"订单商品")))
                    .andExpect(content().string(containsString("href=\"/products?id="+product.getId()+"\"")));
        }
        mvc.perform(get("/api/products/{id}/reviews",product.getId()).session(buyer))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"))
                .andExpect(jsonPath("$.reviews[0].comment").value("Excellent purchase"))
                .andExpect(jsonPath("$.reviews[0].rating").value(4))
                .andExpect(jsonPath("$.totalReviews").value(1)).andExpect(jsonPath("$.averageRating").value(4.0));
        mvc.perform(get("/api/products/{id}",product.getId())).andExpect(jsonPath("$.averageRating").value(4.0))
                .andExpect(jsonPath("$.totalReviews").value(1));
        product.setPrice(new BigDecimal("999.00")); products.saveAndFlush(product);
        assertThat(items.findByOrder_IdOrderByIdAsc(id).get(0).getUnitPrice()).isEqualByComparingTo("75.00");
    }

    @Test void confirmationAndOrderReviewRejectForeignOrdersExpiredFormsAndUnrelatedProducts() throws Exception {
        var buyer = customer(); var outsider = customer(); var product = product(); Long id = buy(buyer, product);
        mvc.perform(post("/orders/{id}/confirm", id).session(outsider).param("cartFormToken", cart.formToken(outsider)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/orders/{id}/confirm", id).session(buyer).param("cartFormToken", "wrong"))
                .andExpect(status().isForbidden());
        assertThat(orders.findById(id).orElseThrow().isReceiptConfirmed()).isFalse();
        mvc.perform(post("/orders/{id}/confirm", id).session(buyer).param("cartFormToken", cart.formToken(buyer))).andExpect(status().isOk());
        mvc.perform(post("/orders/{id}/reviews/{product}", id, product().getId()).session(buyer)
                .param("cartFormToken", cart.formToken(buyer)).param("rating", "5").param("comment", "Not in this order"))
                .andExpect(status().isNotFound());
        assertThat(reviews.read(product.getId(), userId(outsider)).canReview()).isFalse();
    }

    @Test void validationReturnsLocalizedReviewPageErrorWithoutDroppingReviewDraft() throws Exception {
        var buyer = customer(); var product = product(); Long id = buy(buyer, product);
        mvc.perform(post("/orders/{id}/confirm", id).session(buyer).param("cartFormToken", cart.formToken(buyer))).andExpect(status().isOk());
        for (String lang : java.util.List.of("en", "zh")) {
            mvc.perform(post("/orders/{id}/reviews/{product}", id, product.getId()).session(buyer)
                    .cookie(new Cookie("store_lang", lang)).param("cartFormToken", cart.formToken(buyer))
                    .param("rating", "0").param("comment", "My retained draft"))
                    .andExpect(status().isOk()).andExpect(view().name("orders/review"))
                    .andExpect(content().string(containsString(lang.equals("en") ? "Choose 1–5 stars" : "请选择 1–5 星")))
                    .andExpect(content().string(containsString("My retained draft")));
        }
    }
}
