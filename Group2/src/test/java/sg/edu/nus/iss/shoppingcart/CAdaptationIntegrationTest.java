package sg.edu.nus.iss.shoppingcart;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import sg.edu.nus.iss.shoppingcart.dto.CheckoutReceipt;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.interceptor.LoginInterceptor;
import sg.edu.nus.iss.shoppingcart.repository.OrderItemRepository;
import sg.edu.nus.iss.shoppingcart.repository.OrderRepository;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;
import sg.edu.nus.iss.shoppingcart.service.CartService;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 验证新版 A 商品入口与 C 表单接口、B 登录及 D 真实订单事务的衔接。
 * 不包裹测试事务：D 暂停外层事务后仍需读取已经提交的用户和商品。
 * @author Letian Xie
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:shopping_cart_c_adaptation;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class CAdaptationIntegrationTest {
    @Autowired sg.edu.nus.iss.shoppingcart.service.ShippingAddressService deliveryAddresses;
    @Autowired sg.edu.nus.iss.shoppingcart.repository.ShippingAddressRepository deliveryAddressRows;
    private String deliveryAddress(MockHttpSession session) {
        Long userId = (Long) session.getAttribute("loginUserId");
        var existing = deliveryAddresses.listForUser(userId);
        if (!existing.isEmpty()) return existing.get(0).getId().toString();
        var form = new sg.edu.nus.iss.shoppingcart.form.ShippingAddressForm();
        form.setRecipientName("Test Buyer"); form.setPhone("+65 81234567");
        form.setCountry("Singapore"); form.setCity("Singapore");
        form.setPostalCode("123456"); form.setAddressLine1("12 Test Street");
        return deliveryAddresses.saveForUser(userId, null, form).getId().toString();
    }

    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @MockitoSpyBean OrderItemRepository orderItems;
    @Autowired PasswordEncoder encoder;
    @Autowired CartService cart;
    @Autowired DataSource dataSource;

    private User firstUser;
    private User secondUser;
    private Product keyboard;
    private Product mouse;

    @BeforeEach
    void setup() throws Exception {
        cleanup();
        firstUser = createUser("cfirst001");
        secondUser = createUser("csecond001");
        keyboard = createProduct("C Keyboard", "19.90");
        mouse = createProduct("C Mouse", "9.50");
    }

    @AfterEach
    void cleanup() throws Exception {
        // 故障注入也在断言失败时清除，不影响后续测试。
        reset(orderItems);
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL())
                    .startsWith("jdbc:h2:mem:shopping_cart_c_adaptation");
        }
        orderItems.deleteAllInBatch();
        orders.deleteAllInBatch();
        deliveryAddressRows.deleteAllInBatch();
        users.deleteAllInBatch();
        products.deleteAllInBatch();
    }

    @Test
    void anonymousFormApiReturns401WithoutCreatingSession() throws Exception {
        MvcResult result = mvc.perform(get("/api/cart/form"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(orders.count()).isZero();
    }

    @Test
    void expiredFormApiCannotReuseAuthenticatedSession() throws Exception {
        MockHttpSession session = login(firstUser, null);
        add(session, keyboard, "2", formToken(session));
        session.invalidate();
        mvc.perform(get("/api/cart/form").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control", "no-store"));
        assertThat(LoginInterceptor.currentUserId(session)).isNull();
        assertThat(orders.count()).isZero();
    }

    @Test
    void formApiUsesCurrentLoginAndReportsKindsAndTotalQuantity() throws Exception {
        MockHttpSession session = login(firstUser, null);
        String token = formToken(session);
        mvc.perform(get("/api/cart/form").session(session))
                .andExpect(jsonPath("$.itemCount").value(0))
                .andExpect(jsonPath("$.totalQuantity").value(0));
        add(session, keyboard, "2", token);
        add(session, mouse, "3", token);
        mvc.perform(get("/api/cart/form").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartFormToken").value(token))
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.totalQuantity").value(5));
        assertThat(cart.requireUserId(session)).isEqualTo(firstUser.getId());
    }

    @Test
    void angularFormEntryMergesAndDRepricesReceiptWhileDuplicateKeepsNewCart() throws Exception {
        MockHttpSession session = login(firstUser, null);
        String formToken = formToken(session);
        add(session, keyboard, "2", formToken);
        add(session, keyboard, "1", formToken);
        add(session, mouse, "2", formToken);
        mvc.perform(get("/cart").session(session))
                .andExpect(model().attribute("totalQuantity", 5))
                .andExpect(model().attribute("cartTotal", new BigDecimal("78.70")));
        String checkoutToken = checkoutToken(session);

        keyboard.setPrice(new BigDecimal("21.10"));
        products.saveAndFlush(keyboard);
        mvc.perform(post("/checkout").session(session).param("addressId", deliveryAddress(session)).param("checkoutToken", checkoutToken)
                        .param("totalAmount", "0.01").param("userId", secondUser.getId().toString()))
                .andExpect(redirectedUrl("/checkout/success?key=" + checkoutToken));
        var saved = orders.findByCheckoutTokenAndUser_Id(checkoutToken, firstUser.getId()).orElseThrow();
        assertThat(saved.getTotalAmount()).isEqualByComparingTo("82.30");
        assertThat(orders.findByCheckoutTokenAndUser_Id(checkoutToken, secondUser.getId())).isEmpty();
        assertThat(orderItems.findByOrder_IdOrderByIdAsc(saved.getId())).hasSize(2);
        MvcResult success = mvc.perform(get("/checkout/success").session(session).param("key", checkoutToken))
                .andExpect(status().isOk()).andExpect(view().name("orders/checkout-success"))
                .andExpect(content().string(containsString("C Keyboard")))
                .andExpect(content().string(containsString("82.30"))).andReturn();
        CheckoutReceipt receipt = (CheckoutReceipt) success.getModelAndView().getModel().get("receipt");
        assertThat(receipt.id()).isEqualTo(saved.getId());
        assertThat(receipt.items()).extracting(CheckoutReceipt.Line::quantity).containsExactly(3, 2);
        assertThat(receipt.items().get(0).unitPrice()).isEqualByComparingTo("21.10");
        mvc.perform(get("/api/cart/form").session(session))
                .andExpect(jsonPath("$.itemCount").value(0)).andExpect(jsonPath("$.totalQuantity").value(0));

        add(session, keyboard, "1", formToken(session));
        mvc.perform(post("/checkout").session(session).param("addressId", deliveryAddress(session)).param("checkoutToken", checkoutToken))
                .andExpect(redirectedUrl("/checkout/success?key=" + checkoutToken));
        assertThat(orders.count()).isEqualTo(1);
        assertThat(cart.readForCheckout(session)).containsEntry(keyboard.getId(), 1);
        mvc.perform(get("/api/cart/form").session(session))
                .andExpect(jsonPath("$.itemCount").value(1)).andExpect(jsonPath("$.totalQuantity").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "0", "100", "1.5", "abc", "2147483648", ""})
    void invalidQuantityFromAngularFormKeepsExistingCart(String quantity) throws Exception {
        MockHttpSession session = login(firstUser, null);
        String token = formToken(session);
        add(session, keyboard, "1", token);
        mvc.perform(post("/cart/add").session(session).param("productId", keyboard.getId().toString())
                        .param("quantity", quantity).param("cartFormToken", token))
                .andExpect(redirectedUrl("/cart")).andExpect(flash().attributeExists("errorMessage"));
        assertThat(cart.readForCheckout(session)).containsExactly(java.util.Map.entry(keyboard.getId(), 1));
        assertThat(cart.calculateTotal(cart.getCartItems(session))).isEqualByComparingTo("19.90");
        assertThat(orders.count()).isZero();
    }

    @Test
    void mergedQuantityLimitAndZeroUpdateRemainServerValidated() throws Exception {
        MockHttpSession session = login(firstUser, null);
        String token = formToken(session);
        add(session, keyboard, "99", token);
        mvc.perform(post("/cart/add").session(session).param("productId", keyboard.getId().toString())
                        .param("quantity", "1").param("cartFormToken", token))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(cart.readForCheckout(session)).containsEntry(keyboard.getId(), 99);
        mvc.perform(post("/cart/update").session(session).param("productId", keyboard.getId().toString())
                        .param("quantity", "0").param("cartFormToken", token))
                .andExpect(flash().attributeExists("successMessage"));
        assertThat(cart.getCartItems(session)).isEmpty();
        String emptyToken = checkoutToken(session);
        mvc.perform(post("/checkout").session(session).param("addressId", deliveryAddress(session)).param("checkoutToken", emptyToken))
                .andExpect(view().name("orders/checkout"))
                .andExpect(model().attribute("errorMessage", containsString("Your cart has been kept")));
        assertThat(orders.count()).isZero();
    }

    @Test
    void productDeactivatedAfterConfirmationKeepsCartAndSameRequestCanRetry() throws Exception {
        MockHttpSession session = login(firstUser, null);
        add(session, keyboard, "2", formToken(session));
        String token = checkoutToken(session);
        keyboard.setActive(false);
        products.saveAndFlush(keyboard);
        mvc.perform(post("/checkout").session(session).param("addressId", deliveryAddress(session)).param("checkoutToken", token))
                .andExpect(status().isOk()).andExpect(view().name("orders/checkout"))
                .andExpect(model().attribute("canCheckout", false))
                .andExpect(model().attribute("errorMessage", containsString("Your cart has been kept")));
        assertThat(cart.getCartItems(session).get(0).getQuantity()).isEqualTo(2);
        assertThat(checkoutToken(session)).isEqualTo(token);
        assertThat(orders.count()).isZero();

        keyboard.setActive(true);
        products.saveAndFlush(keyboard);
        mvc.perform(post("/checkout").session(session).param("addressId", deliveryAddress(session)).param("checkoutToken", token))
                .andExpect(redirectedUrl("/checkout/success?key=" + token));
        assertThat(orders.count()).isEqualTo(1);
        assertThat(cart.getCartItems(session)).isEmpty();
    }

    @Test
    void orderItemWriteFailureRollsBackDOrderAndKeepsCCartForRetry() throws Exception {
        MockHttpSession session = login(firstUser, null);
        add(session, keyboard, "2", formToken(session));
        String token = checkoutToken(session);
        doThrow(new IllegalStateException("C integration persistence failure"))
                .when(orderItems).saveAllAndFlush(any());
        mvc.perform(post("/checkout").session(session).param("addressId", deliveryAddress(session)).param("checkoutToken", token))
                .andExpect(status().isOk()).andExpect(view().name("orders/checkout"))
                .andExpect(model().attribute("errorMessage", containsString("Your cart has been kept")));
        assertThat(orders.count()).isZero();
        assertThat(orderItems.count()).isZero();
        assertThat(cart.readForCheckout(session)).containsEntry(keyboard.getId(), 2);
        assertThat(checkoutToken(session)).isEqualTo(token);

        reset(orderItems);
        mvc.perform(post("/checkout").session(session).param("addressId", deliveryAddress(session)).param("checkoutToken", token))
                .andExpect(redirectedUrl("/checkout/success?key=" + token));
        assertThat(orders.count()).isEqualTo(1);
        assertThat(orderItems.count()).isEqualTo(1);
        assertThat(cart.getCartItems(session)).isEmpty();
    }

    @Test
    void formTokensAndCartStayIsolatedAcrossSessionsAndNewLoginDropsOldState() throws Exception {
        MockHttpSession first = login(firstUser, null);
        String firstFormToken = formToken(first);
        add(first, keyboard, "2", firstFormToken);
        String oldCheckoutToken = checkoutToken(first);
        MockHttpSession second = login(secondUser, null);
        String secondFormToken = formToken(second);
        assertThat(secondFormToken).isNotEqualTo(firstFormToken);
        mvc.perform(post("/cart/add").session(second).param("productId", mouse.getId().toString())
                        .param("quantity", "1").param("cartFormToken", firstFormToken))
                .andExpect(flash().attributeExists("errorMessage"));
        assertThat(cart.getCartItems(second)).isEmpty();
        add(second, mouse, "1", secondFormToken);
        assertThat(cart.readForCheckout(first)).containsExactly(java.util.Map.entry(keyboard.getId(), 2));
        assertThat(cart.readForCheckout(second)).containsExactly(java.util.Map.entry(mouse.getId(), 1));
        mvc.perform(post("/checkout").session(second).param("addressId", deliveryAddress(second)).param("checkoutToken", oldCheckoutToken))
                .andExpect(view().name("orders/checkout")).andExpect(model().attributeExists("errorMessage"));

        MockHttpSession switched = login(secondUser, first);
        assertThat(first.isInvalid()).isTrue();
        assertThat(switched.getAttribute("loginUserId")).isEqualTo(secondUser.getId());
        assertThat(cart.getCartItems(switched)).isEmpty();
        assertThat(formToken(switched)).isNotEqualTo(firstFormToken);
        assertThat(checkoutToken(switched)).isNotEqualTo(oldCheckoutToken);
        add(switched, keyboard, "1", formToken(switched));
        mvc.perform(post("/logout").session(switched)).andExpect(redirectedUrl("/login?loggedOut"));
        assertThat(switched.isInvalid()).isTrue();
        mvc.perform(get("/api/cart/form").session(switched)).andExpect(status().isUnauthorized());
        MockHttpSession relogged = login(secondUser, null);
        mvc.perform(get("/api/cart/form").session(relogged))
                .andExpect(jsonPath("$.itemCount").value(0)).andExpect(jsonPath("$.totalQuantity").value(0));
        assertThat(cart.readForCheckout(second)).containsEntry(mouse.getId(), 1);
        assertThat(orders.count()).isZero();
    }

    @Test
    void selectedProductEntryShowsOnlyRequestedAProductAndUnfilteredEntryStillWorks() throws Exception {
        MockHttpSession session = login(firstUser, null);
        MvcResult selected = mvc.perform(get("/cart/products").session(session)
                        .param("productId", keyboard.getId().toString()))
                .andExpect(status().isOk()).andExpect(view().name("cart/products"))
                .andExpect(content().string(containsString("C Keyboard")))
                .andExpect(content().string(not(containsString("C Mouse")))).andReturn();
        assertThat((List<?>) selected.getModelAndView().getModel().get("products")).hasSize(1);
        assertThat(cart.getCartItems(session)).isEmpty();
        MvcResult all = mvc.perform(get("/cart/products").session(session))
                .andExpect(status().isOk()).andReturn();
        assertThat((List<?>) all.getModelAndView().getModel().get("products")).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "-1", "0", "9223372036854775808"})
    void invalidSelectedProductHasFriendlyErrorAndDoesNotChangeCart(String productId) throws Exception {
        MockHttpSession session = login(firstUser, null);
        add(session, keyboard, "1", formToken(session));
        mvc.perform(get("/cart/products").session(session).param("productId", productId))
                .andExpect(redirectedUrl("/cart")).andExpect(flash().attributeExists("errorMessage"));
        assertThat(cart.readForCheckout(session)).containsExactly(java.util.Map.entry(keyboard.getId(), 1));
    }

    @Test
    void missingOrInactiveSelectedProductIsRejectedWithoutChangingCart() throws Exception {
        MockHttpSession session = login(firstUser, null);
        add(session, keyboard, "1", formToken(session));
        mouse.setActive(false);
        products.saveAndFlush(mouse);
        for (Long id : List.of(Long.MAX_VALUE, mouse.getId())) {
            mvc.perform(get("/cart/products").session(session).param("productId", id.toString()))
                    .andExpect(redirectedUrl("/cart")).andExpect(flash().attributeExists("errorMessage"));
            mvc.perform(post("/cart/add").session(session).param("productId", id.toString())
                            .param("quantity", "1").param("cartFormToken", formToken(session)))
                    .andExpect(redirectedUrl("/cart")).andExpect(flash().attributeExists("errorMessage"));
        }
        assertThat(cart.readForCheckout(session)).containsExactly(java.util.Map.entry(keyboard.getId(), 1));
        assertThat(orders.count()).isZero();
    }

    @Test
    void loginRedirectKeepsSelectedProductQueryAndUsesNewSession() throws Exception {
        String selectedUrl = "/cart/products?productId=" + keyboard.getId();
        MvcResult guest = mvc.perform(get(selectedUrl))
                .andExpect(redirectedUrl("/login?required")).andReturn();
        MockHttpSession previous = (MockHttpSession) guest.getRequest().getSession(false);
        assertThat(previous).isNotNull();
        MvcResult authenticated = mvc.perform(post("/login").session(previous)
                        .param("username", firstUser.getUsername()).param("password", "demo123"))
                .andExpect(redirectedUrl(selectedUrl)).andReturn();
        MockHttpSession session = (MockHttpSession) authenticated.getRequest().getSession(false);
        assertThat(previous.isInvalid()).isTrue();
        assertThat(session.getAttribute("loginUserId")).isEqualTo(firstUser.getId());
        mvc.perform(get(selectedUrl).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("C Keyboard")))
                .andExpect(content().string(not(containsString("C Mouse"))));
        assertThat(cart.getCartItems(session)).isEmpty();
    }

    private User createUser(String username) {
        return users.saveAndFlush(new User(username, encoder.encode("demo123"), username,
                username + "@example.test"));
    }

    private Product createProduct(String name, String price) {
        Product product = new Product();
        product.setName(name);
        product.setDescription("C adaptation test product");
        product.setPrice(new BigDecimal(price));
        product.setImageUrl("/images/keyboard.png");
        return products.saveAndFlush(product);
    }

    private MockHttpSession login(User user, MockHttpSession previous) throws Exception {
        var request = post("/login").param("username", user.getUsername()).param("password", "demo123");
        if (previous != null) { request.session(previous); }
        MvcResult result = mvc.perform(request).andExpect(status().is3xxRedirection()).andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(LoginInterceptor.currentUserId(session)).isEqualTo(user.getId());
        return session;
    }

    private String formToken(MockHttpSession session) throws Exception {
        MvcResult result = mvc.perform(get("/api/cart/form").session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andReturn();
        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.cartFormToken");
        assertThat(token).isNotBlank();
        return token;
    }

    private void add(MockHttpSession session, Product product, String quantity, String token) throws Exception {
        mvc.perform(post("/cart/add").session(session).param("productId", product.getId().toString())
                        .param("quantity", quantity).param("cartFormToken", token))
                .andExpect(redirectedUrl("/cart")).andExpect(flash().attributeExists("successMessage"));
    }

    private String checkoutToken(MockHttpSession session) throws Exception {
        MvcResult result = mvc.perform(get("/checkout").session(session))
                .andExpect(status().isOk()).andExpect(view().name("orders/checkout")).andReturn();
        String token = (String) result.getModelAndView().getModel().get("checkoutToken");
        assertThat(token).isNotBlank();
        return token;
    }
}
