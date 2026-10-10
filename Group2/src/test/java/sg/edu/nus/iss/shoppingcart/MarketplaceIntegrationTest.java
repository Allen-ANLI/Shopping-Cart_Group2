package sg.edu.nus.iss.shoppingcart;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import sg.edu.nus.iss.shoppingcart.entity.*;
import sg.edu.nus.iss.shoppingcart.dto.ProductForm;
import sg.edu.nus.iss.shoppingcart.repository.*;
import sg.edu.nus.iss.shoppingcart.service.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:marketplace;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class MarketplaceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired OrderRepository orders;
    @Autowired ProductReviewRepository reviews;
    @Autowired ProductService catalog;
    @Autowired ProductReviewService reviewService;
    @Autowired CartService cart;
    @Autowired CheckoutCoordinator checkout;
    @Autowired AdminProductService admin;
    @Autowired RecommendationService recommendations;
    @Autowired sg.edu.nus.iss.shoppingcart.config.ReviewSampleInitializer samples;

    private MockHttpSession customer() {
        User user = users.saveAndFlush(new User("test_" + UUID.randomUUID().toString().substring(0,12), "hash", "Buyer", null));
        var session = new MockHttpSession(); session.setAttribute("loginUserId", user.getId()); return session;
    }
    private void confirm(Long id) { var order = orders.findById(id).orElseThrow(); order.confirmReceipt(); orders.saveAndFlush(order); }
    private Long owner(MockHttpSession session) { return (Long) session.getAttribute("loginUserId"); }
    private Product product(String category, int stock) {
        Product product = new Product(); product.setName("Flow " + UUID.randomUUID());
        product.setCategory(category); product.setPrice(new BigDecimal("12.50")); product.setStockQuantity(stock);
        return products.saveAndFlush(product);
    }
    private String reviewBody(MockHttpSession session) {
        return "{\"rating\":5,\"comment\":\"Very useful product\",\"cartFormToken\":\"" + cart.formToken(session) + "\"}";
    }

    @Test void onlyTheActualBuyerCanRateAndUpdateAReviewAndMetricsAgree() throws Exception {
        var buyer = customer(); var stranger = customer(); var product = product("audio", 10);
        mvc.perform(get("/api/products/{id}/reviews", product.getId()).session(buyer))
                .andExpect(jsonPath("$.canReview").value(false)).andExpect(jsonPath("$.loggedIn").value(true));
        mvc.perform(post("/api/products/{id}/reviews", product.getId()).session(buyer).contentType(MediaType.APPLICATION_JSON)
                .content(reviewBody(buyer))).andExpect(status().isForbidden());
        cart.addItem(buyer, product.getId(), 4); confirm(checkout.submit(buyer, checkout.prepare(buyer)));
        mvc.perform(post("/api/products/{id}/reviews", product.getId()).session(stranger).contentType(MediaType.APPLICATION_JSON)
                .content(reviewBody(stranger).replace("}", ",\"userId\":" + owner(buyer) + "}"))).andExpect(status().isForbidden());
        mvc.perform(post("/api/products/{id}/reviews", product.getId()).session(buyer).contentType(MediaType.APPLICATION_JSON)
                .content(reviewBody(buyer))).andExpect(status().isOk()).andExpect(jsonPath("$.canReview").value(true));
        reviewService.save(product.getId(), owner(buyer), 3, "Updated after more use");
        mvc.perform(get("/api/products/{id}", product.getId())).andExpect(jsonPath("$.averageRating").value(3.0))
                .andExpect(jsonPath("$.totalReviews").value(1)).andExpect(jsonPath("$.salesCount").value(4))
                .andExpect(jsonPath("$.stockQuantity").value(6));
        mvc.perform(get("/api/products").param("page", "0").param("q", product.getName()))
                .andExpect(jsonPath("$.content[0].averageRating").value(3.0)).andExpect(jsonPath("$.content[0].salesCount").value(4));
    }

    @Test void declineAndInvalidPaymentPreserveStockAndCartThenRetryIsIdempotent() {
        var session = customer(); var product = product("typing", 3); cart.addItem(session, product.getId(), 2);
        String token = checkout.prepare(session); long baseline = orders.count();
        for (PaymentService.Request request : List.of(new PaymentService.Request("VISA", "DECLINED"),
                new PaymentService.Request("FORGED", "APPROVED"))) {
            assertThatThrownBy(() -> checkout.submit(session, token, null, request)).isInstanceOf(RuntimeException.class);
            assertThat(orders.count()).isEqualTo(baseline);
            assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);
            assertThat(cart.readForCheckout(session)).containsEntry(product.getId(), 2);
            assertThat(reviewService.read(product.getId(), owner(session)).canReview()).isFalse();
        }
        Long id = checkout.submit(session, token, null, new PaymentService.Request("VISA", "APPROVED"));
        assertThat(checkout.submit(session, token, null, new PaymentService.Request("VISA", "APPROVED"))).isEqualTo(id);
        assertThat(orders.count()).isEqualTo(baseline + 1);
        var order = orders.findById(id).orElseThrow();
        assertThat(order.getPaymentStatus()).isEqualTo("PAID"); assertThat(order.getPaymentMethod()).isEqualTo("VISA");
        assertThat(order.getPaymentReference()).startsWith("TXN-"); assertThat(order.getPaidAt()).isNotNull();
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(1);
        assertThat(cart.countItems(session)).isZero();
    }

    @Test void concurrentCustomersCannotBuyTheSameLastUnit() throws Exception {
        var product = product("audio", 1); var first = customer(); var second = customer();
        cart.addItem(first, product.getId(), 1); cart.addItem(second, product.getId(), 1);
        String firstToken = checkout.prepare(first), secondToken = checkout.prepare(second);
        var pool = Executors.newFixedThreadPool(2); var start = new CountDownLatch(1);
        try {
            Callable<Boolean> buyFirst = () -> { start.await(); try { checkout.submit(first, firstToken); return true; } catch (RuntimeException e) { return false; } };
            Callable<Boolean> buySecond = () -> { start.await(); try { checkout.submit(second, secondToken); return true; } catch (RuntimeException e) { return false; } };
            var a = pool.submit(buyFirst); var b = pool.submit(buySecond); start.countDown();
            assertThat(List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS))).containsExactlyInAnyOrder(true, false);
            assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isZero();
            assertThat(products.findById(product.getId()).orElseThrow().getSalesCount()).isEqualTo(1);
            assertThat(cart.countItems(first) + cart.countItems(second)).isEqualTo(1);
        } finally { pool.shutdownNow(); }
    }

    @Test void quantityApiPersistsChangesRejectsInvalidInputAndZeroRemoves() throws Exception {
        var session = customer(); var product = product("typing", 5); cart.addItem(session, product.getId(), 1);
        String token = cart.formToken(session);
        for (int quantity : new int[]{3, 2, 0}) {
            mvc.perform(put("/api/cart/items/{id}", product.getId()).session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"quantity\":" + quantity + ",\"cartFormToken\":\"" + token + "\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalQuantity").value(quantity));
            mvc.perform(get("/api/cart/form").session(session)).andExpect(jsonPath("$.totalQuantity").value(quantity));
        }
        assertThat(cart.countItems(session)).isZero();
        cart.addItem(session, product.getId(), 1);
        for (String body : List.of("{\"quantity\":6,\"cartFormToken\":\"" + token + "\"}", "{\"quantity\":0,\"cartFormToken\":\"wrong\"}")) {
            mvc.perform(put("/api/cart/items/{id}", product.getId()).session(session).contentType(MediaType.APPLICATION_JSON)
                    .content(body)).andExpect(status().isBadRequest());
        }
        assertThat(cart.readForCheckout(session)).containsEntry(product.getId(), 1);
    }

    @Test void administratorsManageStockAndAutoHideRestoresOnRestock() throws Exception {
        var form = new ProductForm(); form.setName("Stock test " + UUID.randomUUID()); form.setPrice(BigDecimal.TEN);
        form.setStockQuantity(0); form.setHideWhenOutOfStock(true); var product = admin.create(form);
        assertThat(catalog.findProductById(product.getId())).isEmpty();
        assertThat(catalog.findAllProducts(null, form.getName(), "featured")).isEmpty();
        assertThatThrownBy(() -> cart.addItem(customer(), product.getId(), 1)).isInstanceOf(RuntimeException.class);
        form.setStockQuantity(4); admin.update(product.getId(), form);
        assertThat(catalog.findProductById(product.getId())).isPresent();
        form.setStockQuantity(-1); assertThatThrownBy(() -> admin.update(product.getId(), form)).isInstanceOf(RuntimeException.class);
        mvc.perform(get("/admin/products").session(customer())).andExpect(redirectedUrl("/forbidden"));
        admin.toggleActive(product.getId()); assertThat(catalog.findProductById(product.getId())).isEmpty();
        admin.deleteIfUnreferenced(product.getId()); assertThat(products.findById(product.getId())).isEmpty();
    }

    @Test void sortsBySalesAndRatingAndRemovesLegacyOptions() throws Exception {
        var buyer = customer(); var first = product("typing", 10); var second = product("typing", 10);
        String marker = UUID.randomUUID().toString(); first.setName(marker + " A"); second.setName(marker + " B");
        products.saveAllAndFlush(List.of(first, second));
        cart.addItem(buyer, first.getId(), 3); cart.addItem(buyer, second.getId(), 1); confirm(checkout.submit(buyer, checkout.prepare(buyer)));
        reviewService.save(first.getId(), owner(buyer), 3, "Useful for everyday work");
        reviewService.save(second.getId(), owner(buyer), 5, "Excellent for everyday work");
        mvc.perform(get("/api/products").param("page", "0").param("q", marker).param("sort", "sales"))
                .andExpect(jsonPath("$.content[0].id").value(first.getId()));
        mvc.perform(get("/api/products").param("page", "0").param("q", marker).param("sort", "rating"))
                .andExpect(jsonPath("$.content[0].id").value(second.getId()));
        mvc.perform(get("/api/products").param("page", "0").param("sort", "newest")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("page", "0").param("sort", "name")).andExpect(status().isBadRequest());
    }

    @Test void recommendationsUsePreferencesBeforePagingAndRespectFilters() throws Exception {
        var session = customer(); var audio = product("audio", 5);
        recommendations.remember(session, audio);
        var ranked = recommendations.page(0, 6, null, null, session);
        assertThat(ranked.getContent()).allMatch(p -> p.getCategory().equals("audio"));
        assertThat(recommendations.page(0, 6, null, null, session).getContent()).extracting(Product::getId)
                .containsExactlyElementsOf(ranked.getContent().stream().map(Product::getId).toList());
        mvc.perform(get("/api/products").session(session).param("page", "0").param("sort", "recommended").param("category", "typing"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].category").value("typing"));
        var buyer = customer(); var typing = product("typing", 5); cart.addItem(buyer, typing.getId(), 1);
        confirm(checkout.submit(buyer, checkout.prepare(buyer))); reviewService.save(typing.getId(), owner(buyer), 5, "Very comfortable keyboard");
        assertThat(recommendations.page(0, 6, null, null, buyer).getContent()).allMatch(p -> p.getCategory().equals("typing"));
    }

    @Test void navigationHighlightsTheCurrentSectionInBothLanguages() throws Exception {
        var session = customer();
        for (String path : List.of("/cart", "/orders", "/account", "/account/addresses")) {
            String active = path.startsWith("/account") ? "/account" : path;
            for (String language : List.of("en", "zh")) {
                String html = mvc.perform(get(path).session(session).cookie(new jakarta.servlet.http.Cookie("store_lang", language)))
                        .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
                assertThat(java.util.regex.Pattern.compile("<a\\b(?=[^>]*href=\"" + active
                        + "\")(?=[^>]*aria-current=\"page\")[^>]*>").matcher(html).find()).isTrue();
            }
        }
    }

    @Test void samplesAreLabelledAndDoNotDuplicateOnStartup() throws Exception {
        long reviewCount = reviews.count(), orderCount = orders.count(); samples.run();
        assertThat(reviews.count()).isEqualTo(reviewCount); assertThat(orders.count()).isEqualTo(orderCount);
        var sample = reviews.findAll().stream().filter(ProductReview::isSample).findFirst().orElseThrow();
        mvc.perform(get("/api/products/{id}/reviews", sample.getProduct().getId())).andExpect(status().isOk())
                .andExpect(content().string(containsString("\"sample\":true"))).andExpect(jsonPath("$.canReview").value(false));
    }
}
