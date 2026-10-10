package sg.edu.nus.iss.shoppingcart;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import sg.edu.nus.iss.shoppingcart.entity.*;
import sg.edu.nus.iss.shoppingcart.form.ShippingAddressForm;
import sg.edu.nus.iss.shoppingcart.repository.*;
import sg.edu.nus.iss.shoppingcart.service.*;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:delivery_tests;MODE=MySQL;DB_CLOSE_DELAY=-1"})
@AutoConfigureMockMvc
class DeliveryCheckoutTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired ShippingAddressService addresses;
    @Autowired CartService cart;
    @Autowired CheckoutCoordinator checkout;

    private MockHttpSession customer() {
        User user = users.saveAndFlush(new User("u" + UUID.randomUUID().toString().substring(0,12), "hash", "Buyer", "buyer@example.com"));
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUserId", user.getId());
        return session;
    }
    private ShippingAddress address(MockHttpSession session, String street) {
        ShippingAddressForm form = new ShippingAddressForm();
        form.setRecipientName("Test Recipient"); form.setPhone("+65 81234567");
        form.setCountry("Singapore"); form.setCity("Singapore"); form.setPostalCode("123456");
        form.setAddressLine1(street); form.setDefaultAddress(true);
        return addresses.saveForUser((Long)session.getAttribute("loginUserId"), null, form);
    }
    private Product product() {
        Product p = new Product(); p.setName("Delivery test product"); p.setPrice(new BigDecimal("12.30"));
        return products.saveAndFlush(p);
    }
    @Test void addressRequiredAndOtherUsersAddressCannotCreateAnOrder() throws Exception {
        var session = customer(); var foreign = address(customer(), "Other street");
        cart.addItem(session, product().getId(), 1); String token = checkout.prepare(session);
        mvc.perform(get("/checkout").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Add a delivery address")));
        mvc.perform(post("/checkout").session(session).param("checkoutToken", token))
                .andExpect(view().name("orders/checkout")).andExpect(model().attributeExists("errorMessage"));
        mvc.perform(post("/checkout").session(session).param("checkoutToken", token).param("addressId", foreign.getId().toString()))
                .andExpect(view().name("orders/checkout")).andExpect(model().attributeExists("errorMessage"));
        assertThat(orders.findByCheckoutTokenAndUser_Id(token, (Long)session.getAttribute("loginUserId"))).isEmpty();
        assertThat(cart.countItems(session)).isEqualTo(1);
    }
    @Test void completedOrderKeepsAddressSnapshotAfterEditDeleteAndDuplicateSubmit() throws Exception {
        var session = customer(); var address = address(session, "Original street 12");
        Long owner = (Long)session.getAttribute("loginUserId");
        cart.addItem(session, product().getId(), 2); String token = checkout.prepare(session);
        mvc.perform(post("/checkout").session(session).param("checkoutToken", token).param("addressId", address.getId().toString()))
                .andExpect(redirectedUrl("/checkout/success?key=" + token));
        var order = orders.findByCheckoutTokenAndUser_Id(token, owner).orElseThrow();
        var edit = ShippingAddressForm.from(address); edit.setAddressLine1("Changed street 99");
        addresses.saveForUser(owner, address.getId(), edit); addresses.deleteForUser(owner, address.getId());
        mvc.perform(get("/orders/" + order.getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Original street 12")));
        mvc.perform(post("/checkout").session(session).param("checkoutToken", token).param("addressId", address.getId().toString()))
                .andExpect(redirectedUrl("/checkout/success?key=" + token));
        assertThat(orders.findByCheckoutTokenAndUser_Id(token, owner).orElseThrow().getId()).isEqualTo(order.getId());
        assertThat(cart.countItems(session)).isZero();
        mvc.perform(get("/checkout/success").session(session).param("key", token))
                .andExpect(content().string(containsString("Original street 12")));
    }
    @Test void quickAddIsJsonValidatedAndDoesNotNavigateAway() throws Exception {
        var session = customer(); var product = product(); String token = cart.formToken(session);
        mvc.perform(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/cart/items").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":"+product.getId()+",\"quantity\":1,\"cartFormToken\":\"wrong\"}"))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        for (int count=1;count<=2;count++) {
            mvc.perform(post("/api/cart/items").session(session).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"productId\":"+product.getId()+",\"quantity\":1,\"cartFormToken\":\""+token+"\"}"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalQuantity").value(count));
        }
        mvc.perform(post("/api/cart/items").session(session).contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":"+product.getId()+",\"quantity\":100,\"cartFormToken\":\""+token+"\"}"))
                .andExpect(status().isBadRequest());
        assertThat(cart.readForCheckout(session)).containsEntry(product.getId(),2);
    }
    @Test void cookieLanguageAppliesAcrossServerPages() throws Exception {
        var session = customer();
        mvc.perform(get("/cart").session(session).cookie(new Cookie("store_lang", "zh")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("购物车还是空的")));
        mvc.perform(get("/checkout").session(session).cookie(new Cookie("store_lang", "zh")))
                .andExpect(content().string(containsString("确认订单")));
        mvc.perform(get("/orders").session(session).cookie(new Cookie("store_lang", "zh")))
                .andExpect(content().string(containsString("购买历史")));
        mvc.perform(get("/cart").session(session).cookie(new Cookie("store_lang", "en")))
                .andExpect(content().string(containsString("Your cart is empty")));
    }
}
