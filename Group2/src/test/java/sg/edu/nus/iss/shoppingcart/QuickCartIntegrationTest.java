package sg.edu.nus.iss.shoppingcart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;
import sg.edu.nus.iss.shoppingcart.service.CartService;

import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class QuickCartIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired CartService cart;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    MockHttpSession session;
    Product product;
    String token;

    @BeforeEach void setup() {
        User user = users.saveAndFlush(new User("quickcart", "unused", "Quick cart", "quick@example.com"));
        product = new Product(); product.setName("Quick mouse"); product.setPrice(new BigDecimal("20.00"));
        product = products.saveAndFlush(product);
        session = new MockHttpSession(); session.setAttribute("loginUserId", user.getId());
        token = cart.formToken(session);
    }

    @Test void readsQuantitiesAddedThroughTheExistingCartForm() throws Exception {
        mvc.perform(post("/cart/add").session(session).param("productId", product.getId().toString())
                .param("quantity", "3").param("cartFormToken", token)).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/cart/state").session(session)).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.quantities['" + product.getId() + "']").value(3))
                .andExpect(jsonPath("$.totalQuantity").value(3));
    }

    @Test void adjustsTheCurrentServerQuantityAndRemovesAtZero() throws Exception {
        cart.addItem(session, product.getId(), 4);
        adjust("1", token).andExpect(status().isOk()).andExpect(jsonPath("$.totalQuantity").value(5));
        // Another page changes the quantity before this card's next click.
        cart.updateQuantity(session, product.getId(), 1);
        adjust("-1", token).andExpect(status().isOk()).andExpect(jsonPath("$.totalQuantity").value(0));
        assertThat(cart.getCartItems(session)).isEmpty();
        adjust("-1", token).andExpect(status().isOk()).andExpect(jsonPath("$.totalQuantity").value(0));
        adjust("1", token).andExpect(status().isOk()).andExpect(jsonPath("$.totalQuantity").value(1));
    }

    @Test void rejectsMissingTokenInvalidDeltaAndQuantityOverflow() throws Exception {
        adjust("1", "wrong").andExpect(status().isBadRequest());
        adjust("2", token).andExpect(status().isBadRequest());
        assertThat(cart.getCartItems(session)).isEmpty();
        cart.addItem(session, product.getId(), 99);
        adjust("1", token).andExpect(status().isBadRequest());
        assertThat(cart.readForCheckout(session)).containsEntry(product.getId(), 99);
        product.setActive(false); products.saveAndFlush(product);
        adjust("-1", token).andExpect(status().isOk()).andExpect(jsonPath("$.totalQuantity").value(98));
        adjust("1", token).andExpect(status().isBadRequest());
    }

    @Test void requiresAuthenticationAndKeepsSessionsSeparate() throws Exception {
        mvc.perform(get("/api/cart/state")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/cart/adjust").param("productId", product.getId().toString())
                .param("delta", "1").param("cartFormToken", token)).andExpect(status().isUnauthorized());
        cart.addItem(session, product.getId(), 2);
        MockHttpSession other = new MockHttpSession();
        other.setAttribute("loginUserId", session.getAttribute("loginUserId"));
        mvc.perform(get("/api/cart/state").session(other)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalQuantity").value(0));
    }

    private org.springframework.test.web.servlet.ResultActions adjust(String delta, String suppliedToken) throws Exception {
        return mvc.perform(post("/api/cart/adjust").session(session).param("productId", product.getId().toString())
                .param("delta", delta).param("cartFormToken", suppliedToken));
    }
}
