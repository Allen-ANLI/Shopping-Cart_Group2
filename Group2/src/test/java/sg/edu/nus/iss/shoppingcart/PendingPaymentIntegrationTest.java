package sg.edu.nus.iss.shoppingcart;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import sg.edu.nus.iss.shoppingcart.entity.*;
import sg.edu.nus.iss.shoppingcart.repository.*;
import sg.edu.nus.iss.shoppingcart.service.*;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="spring.datasource.url=jdbc:h2:mem:pending-payment;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class PendingPaymentIntegrationTest {
    @Autowired MockMvc mvc; @Autowired UserRepository users; @Autowired ProductRepository products;
    @Autowired OrderRepository orders; @Autowired OrderItemRepository items; @Autowired ProductReviewRepository reviews;
    @Autowired CartService cart; @Autowired CheckoutCoordinator checkout; @Autowired OrderPaymentService payments;
    @Autowired ShippingAddressService addresses;
    MockHttpSession customer(){var user=users.saveAndFlush(new User("pending_"+UUID.randomUUID().toString().substring(0,8),"hash","Buyer",null));var session=new MockHttpSession();session.setAttribute("loginUserId",user.getId());return session;}
    Long owner(MockHttpSession session){return (Long)session.getAttribute("loginUserId");}
    Product product(int stock){var p=new Product();p.setName("Portable display 显示器");p.setPrice(new BigDecimal("99.99"));p.setDiscountPercent(25);p.setStockQuantity(stock);return products.saveAndFlush(p);}
    Long pending(MockHttpSession session,Product product,int quantity){cart.addItem(session,product.getId(),quantity);return checkout.placeOrder(session,checkout.prepare(session),null);}
    @Test void checkoutCreatesPendingOrderThenPaysSnapshotAndUnlocksSeparateReviewPage() throws Exception {
        var session=customer();var product=product(10);
        var address=new sg.edu.nus.iss.shoppingcart.form.ShippingAddressForm();address.setRecipientName("林先生");address.setPhone("+65 81234567");address.setCountry("Singapore");address.setCity("Singapore");address.setPostalCode("123456");address.setAddressLine1("12 Example Street");
        var savedAddress=addresses.saveForUser(owner(session),null,address);
        cart.addItem(session,product.getId(),3);String token=checkout.prepare(session);
        var response=mvc.perform(post("/checkout").session(session).param("checkoutToken",token).param("addressId",savedAddress.getId().toString()))
            .andExpect(redirectedUrlPattern("/orders/*/payment")).andReturn();
        var order=orders.findByCheckoutTokenAndUser_Id(token,owner(session)).orElseThrow();Long id=order.getId();
        assertThat(order.getPaymentStatus()).isEqualTo("PENDING");assertThat(order.getShipmentDeliveredAt()).isNull();
        assertThat(order.getOriginalSubtotal()).isEqualByComparingTo("299.97");assertThat(order.getDiscountAmount()).isEqualByComparingTo("75.00");
        assertThat(order.getGstAmount()).isEqualByComparingTo("20.25");assertThat(order.getTotalAmount()).isEqualByComparingTo("245.22");
        assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(10);assertThat(cart.countItems(session)).isZero();
        mvc.perform(get(response.getResponse().getRedirectedUrl()).session(session)).andExpect(status().isOk())
            .andExpect(content().string(containsString("245.22"))).andExpect(content().string(not(containsString("paymentPin"))))
            .andExpect(content().string(not(containsString("data-demo-card"))));
        mvc.perform(get("/orders/{id}/reviews/{pid}",id,product.getId()).session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/orders/{id}/confirm",id).session(session).param("cartFormToken",cart.formToken(session))).andExpect(status().isForbidden());
        mvc.perform(get("/orders/{id}/receipt.pdf",id).session(session)).andExpect(status().isConflict());
        product.setPrice(new BigDecimal("200.00"));product.setDiscountPercent(0);products.saveAndFlush(product);
        for(int retry=0;retry<2;retry++) mvc.perform(post("/orders/{id}/payment",id).session(session).param("cartFormToken",cart.formToken(session))
            .param("paymentMethod","MASTERCARD").param("cardholderName","Buyer").param("cardNumber","5555 5555 5555 4444")
            .param("cardExpiry","12/99").param("cardSecurityCode","123").param("paymentOutcome","DECLINED"))
            .andExpect(redirectedUrl("/orders/"+id+"?paid"));
        order=orders.findById(id).orElseThrow();assertThat(order.getPaymentStatus()).isEqualTo("PAID");assertThat(order.getShipmentDeliveredAt()).isNotNull();
        assertThat(order.getTotalAmount()).isEqualByComparingTo("245.22");assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(7);
        var pdf=mvc.perform(get("/orders/{id}/receipt.pdf",id).session(session)).andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
            .andExpect(header().string("Cache-Control","no-store")).andExpect(header().string("Content-Disposition",containsString("attachment"))).andReturn().getResponse().getContentAsByteArray();
        try(var document=org.apache.pdfbox.Loader.loadPDF(pdf)){var text=new org.apache.pdfbox.text.PDFTextStripper().getText(document);
            assertThat(text).contains("245.22","20.25","75.00","Mastercard","4444","显示器","林先生").doesNotContain("5555555555554444","simulat");
            java.nio.file.Files.createDirectories(java.nio.file.Path.of(".local/logs"));java.nio.file.Files.write(java.nio.file.Path.of(".local/logs/receipt-verified.pdf"),pdf);
            javax.imageio.ImageIO.write(new org.apache.pdfbox.rendering.PDFRenderer(document).renderImageWithDPI(0,120),"png",java.nio.file.Path.of(".local/logs/receipt-verified.png").toFile());
        }
        mvc.perform(get("/orders/{id}/reviews/{pid}",id,product.getId()).session(session)).andExpect(status().isForbidden());
        mvc.perform(post("/orders/{id}/confirm",id).session(session).param("cartFormToken",cart.formToken(session))).andExpect(status().isOk()).andExpect(content().string(containsString("review-action")));
        mvc.perform(get("/orders/{id}/reviews/{pid}",id,product.getId()).session(session)).andExpect(status().isOk()).andExpect(content().string(containsString("rating-stars")));
        mvc.perform(post("/orders/{id}/reviews/{pid}",id,product.getId()).session(session).param("cartFormToken",cart.formToken(session)).param("rating","5").param("comment","Great everyday display"))
            .andExpect(redirectedUrl("/orders/"+id+"?reviewed"));
        mvc.perform(get("/orders/{id}",id).session(session)).andExpect(content().string(containsString("reviewed-badge"))).andExpect(content().string(not(containsString("class=\"review-action\""))));
    }
    @Test void pendingPaymentIsOwnedTokenProtectedAndValidationPreservesInventory() throws Exception {
        var session=customer();var stranger=customer();var product=product(3);var id=pending(session,product,2);
        for(String path:List.of("/orders/"+id+"/payment","/orders/"+id+"/receipt.pdf")) mvc.perform(get(path).session(stranger)).andExpect(status().isNotFound());
        mvc.perform(post("/orders/{id}/payment",id).session(session).param("cartFormToken","bad")).andExpect(status().isForbidden());
        mvc.perform(post("/orders/{id}/payment",id).session(stranger).param("cartFormToken",cart.formToken(stranger))).andExpect(status().isNotFound());
        mvc.perform(post("/orders/{id}/payment",id).session(session).param("cartFormToken",cart.formToken(session)).param("cardNumber","invalid"))
            .andExpect(status().isOk()).andExpect(view().name("orders/payment")).andExpect(model().attributeExists("errorMessage"));
        assertThat(orders.findById(id).orElseThrow().isPendingPayment()).isTrue();assertThat(products.findById(product.getId()).orElseThrow().getStockQuantity()).isEqualTo(3);
    }
    @Test void concurrentPendingPaymentsCannotOversell()throws Exception {
        var p=product(1);var first=customer();var second=customer();var a=pending(first,p,1);var b=pending(second,p,1);
        var pool=Executors.newFixedThreadPool(2);var start=new CountDownLatch(1);
        try{
            var one=pool.submit(()->{start.await();try{payments.pay(a,owner(first),PaymentService.Request.success());return true;}catch(RuntimeException ex){return false;}});
            var two=pool.submit(()->{start.await();try{payments.pay(b,owner(second),PaymentService.Request.success());return true;}catch(RuntimeException ex){return false;}});start.countDown();
            assertThat(List.of(one.get(20,TimeUnit.SECONDS),two.get(20,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            assertThat(products.findById(p.getId()).orElseThrow().getStockQuantity()).isZero();
        }finally{pool.shutdownNow();}
    }
    @Test void seededReviewsAreMonolingualAndPredominantlyEnglish(){
        var seeded=reviews.findAll().stream().filter(ProductReview::isSample).toList();
        long english=0;for(var review:seeded){String text=review.getComment();boolean chinese=text.codePoints().anyMatch(cp->Character.UnicodeScript.of(cp)==Character.UnicodeScript.HAN);boolean latin=text.matches(".*[A-Za-z].*");assertThat(chinese && latin).isFalse();if(latin)english++;}
        assertThat(english).isGreaterThan(seeded.size()*2L/3);
    }
}
