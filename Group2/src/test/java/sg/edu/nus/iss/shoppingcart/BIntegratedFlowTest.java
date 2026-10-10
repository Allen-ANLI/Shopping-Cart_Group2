package sg.edu.nus.iss.shoppingcart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import sg.edu.nus.iss.shoppingcart.entity.Order;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.form.ProfileForm;
import sg.edu.nus.iss.shoppingcart.form.RegisterForm;
import sg.edu.nus.iss.shoppingcart.interceptor.LoginInterceptor;
import sg.edu.nus.iss.shoppingcart.repository.OrderItemRepository;
import sg.edu.nus.iss.shoppingcart.repository.OrderRepository;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;
import sg.edu.nus.iss.shoppingcart.service.AuthService;
import sg.edu.nus.iss.shoppingcart.service.CartService;
import sg.edu.nus.iss.shoppingcart.service.CheckoutCoordinator;

import java.math.BigDecimal;
import java.util.Collections;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** B 在 A/C/D/E 真实类上的接入回归测试，无伪造业务 Controller。
 * @author luopeiwen
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
class BIntegratedFlowTest {
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
    @Autowired OrderItemRepository items;
    @Autowired PasswordEncoder encoder;
    @Autowired AuthService auth;
    @Autowired CartService cart;
    @Autowired CheckoutCoordinator checkout;
    @Autowired javax.sql.DataSource dataSource;

    User customer;
    User other;
    User admin;
    Product product;

    @BeforeEach
    void setup() throws Exception {
        cleanup();
        customer = createUser("buser001", User.Role.CUSTOMER);
        other = createUser("buser002", User.Role.CUSTOMER);
        admin = createUser("badmin001", User.Role.ADMIN);
        product = new Product();
        product.setName("B integration product");
        product.setDescription("A entity / C cart / D checkout / E history");
        product.setPrice(new BigDecimal("50.00"));
        product.setImageUrl("/images/keyboard.png");
        product = products.saveAndFlush(product);
    }

    // D 的 CheckoutService 会暂停外层事务，所以测试账号必须先真实提交。
    // 此处只清理 @ActiveProfiles("h2") 的隔离内存数据，不接触 MySQL。
    @AfterEach
    void cleanup() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getURL()).startsWith("jdbc:h2:mem:");
        }
        items.deleteAllInBatch();
        orders.deleteAllInBatch();
        deliveryAddressRows.deleteAllInBatch();
        users.deleteAllInBatch();
        products.deleteAllInBatch();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/cart", "/cart/products", "/checkout", "/checkout/success?key=x",
            "/orders", "/orders/1", "/account", "/admin/products", "/admin/products/new"})
    void guestPagesRequireLogin(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?required"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/cart/add", "/cart/update", "/cart/remove", "/cart/clear",
            "/checkout", "/account/profile", "/admin/products"})
    void guestPostsNeverEnterBusinessControllers(String path) throws Exception {
        long count = orders.count();
        mvc.perform(post(path)).andExpect(redirectedUrl("/login?required"));
        assertThat(orders.count()).isEqualTo(count);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/cart", "/api/checkout", "/api/orders", "/api/account", "/api/admin/products"})
    void guestApisReturnJson401(String path) throws Exception {
        mvc.perform(get(path)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("LOGIN_REQUIRED"));
    }

    @Test
    void validLoginUsesAHashAndCreatesOnlyLongIdentityInNewSession() throws Exception {
        MockHttpSession previous = new MockHttpSession();
        previous.setAttribute("cart", "old cart");
        previous.setAttribute("checkoutState", "old checkout");
        previous.setAttribute("loginUserId", other.getId());
        String previousId = previous.getId();
        MockHttpSession session = login(" BUSER001 ", previous);
        assertThat(previous.isInvalid()).isTrue();
        assertThat(session.getId()).isNotEqualTo(previousId);
        assertThat(session.getAttribute("loginUserId")).isEqualTo(customer.getId()).isInstanceOf(Long.class);
        assertThat(Collections.list(session.getAttributeNames())).containsExactly("loginUserId");
        assertThat(session.getAttribute("cart")).isNull();
        assertThat(encoder.matches("demo123", users.findById(customer.getId()).orElseThrow().getPasswordHash())).isTrue();
    }

    @Test
    void badLoginDoesNotCreateIdentityOrEchoPassword() throws Exception {
        var result = mvc.perform(post("/login").param("username", customer.getUsername())
                        .param("password", "wrong-secret"))
                .andExpect(status().isOk()).andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("Invalid username or password")))
                .andExpect(content().string(not(containsString("wrong-secret")))).andReturn();
        assertThat(LoginInterceptor.currentUserId((MockHttpSession) result.getRequest().getSession(false))).isNull();
    }

    @Test
    void invalidLoginInputIsRejectedServerSide() throws Exception {
        mvc.perform(post("/login").param("username", " ").param("password", " "))
                .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("loginForm", "username", "password"));
        assertThat(auth.authenticate(null, "demo123")).isEmpty();
        assertThat(auth.authenticate(customer.getUsername(), "汉".repeat(30))).isEmpty();
    }

    @Test
    void plainPasswordInPasswordHashIsNotAccepted() {
        customer.setPasswordHash("demo123");
        users.saveAndFlush(customer);
        assertThat(auth.authenticate(customer.getUsername(), "demo123")).isEmpty();
    }

    @Test
    void savedGetReturnsToCartAfterLogin() throws Exception {
        var guest = mvc.perform(get("/cart/products")).andReturn();
        MockHttpSession session = (MockHttpSession) guest.getRequest().getSession(false);
        mvc.perform(post("/login").session(session).param("username", customer.getUsername()).param("password", "demo123"))
                .andExpect(redirectedUrl("/cart/products"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void interruptedCheckoutPostIsNotReplayedAfterLogin() throws Exception {
        var guest = mvc.perform(post("/checkout").param("checkoutToken", "attacker-token")).andReturn();
        MockHttpSession session = (MockHttpSession) guest.getRequest().getSession(false);
        var request = post("/login").param("username", customer.getUsername()).param("password", "demo123");
        if (session != null) { request.session(session); }
        mvc.perform(request).andExpect(redirectedUrl("/products"));
        assertThat(orders.count()).isZero();
    }

    @Test
    void externalSavedRedirectCannotBeUsed() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginInterceptor.REDIRECT_AFTER_LOGIN, "https://example.com/");
        mvc.perform(post("/login").session(session).param("username", customer.getUsername()).param("password", "demo123"))
                .andExpect(redirectedUrl("/products"));
    }

    @Test
    void customerDoesNotReturnToSavedAdminPage() throws Exception {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginInterceptor.REDIRECT_AFTER_LOGIN, "/admin/products");
        mvc.perform(post("/login").session(session).param("username", customer.getUsername()).param("password", "demo123"))
                .andExpect(redirectedUrl("/products"));
    }

    @Test
    void registrationUsesExistingAppUsersAndCannotGrantRoleOrChooseId() throws Exception {
        mvc.perform(post("/register").param("username", "NewBUser").param("password", "newpass123")
                        .param("confirmPassword", "newpass123").param("displayName", "New user")
                        .param("fullName", "New Customer").param("phone", "+65 9123 4567").param("birthday", "1998-05-12")
                        .param("email", "new@example.test").param("role", "ADMIN")
                        .param("id", admin.getId().toString()).param("passwordHash", "injected"))
                .andExpect(redirectedUrl("/login?registered"));
        User saved = users.findByUsername("newbuser").orElseThrow();
        assertThat(saved.getId()).isNotEqualTo(admin.getId());
        assertThat(saved.getRole()).isEqualTo(User.Role.CUSTOMER);
        assertThat(saved.getFullName()).isEqualTo("New Customer");
        assertThat(saved.getPhone()).isEqualTo("+65 9123 4567");
        assertThat(saved.getBirthday()).isEqualTo(java.time.LocalDate.of(1998, 5, 12));
        assertThat(saved.getPasswordHash()).startsWith("$2").isNotEqualTo("newpass123");
        assertThat(encoder.matches("newpass123", saved.getPasswordHash())).isTrue();
    }

    @Test
    void duplicateUsernameCaseIsRejected() throws Exception {
        long count = users.count();
        mvc.perform(post("/register").param("username", customer.getUsername().toUpperCase())
                        .param("password", "newpass123").param("confirmPassword", "newpass123")
                        .param("fullName", "Duplicate Customer").param("phone", "+65 9123 4567")
                        .param("displayName", "Duplicated").param("email", "new@example.test"))
                .andExpect(view().name("auth/register"))
                .andExpect(content().string(containsString("already taken")));
        assertThat(users.count()).isEqualTo(count);
    }

    @Test
    void invalidRegisterFieldsAndMismatchedPasswordsAreRejected() throws Exception {
        mvc.perform(post("/register").param("username", "bad!").param("password", "secret123")
                        .param("confirmPassword", "different123").param("displayName", " ")
                        .param("email", "invalid"))
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "username", "displayName", "email", "passwordConfirmed"))
                .andExpect(content().string(not(containsString("secret123"))))
                .andExpect(content().string(not(containsString("different123"))));
    }

    @Test
    void multibytePasswordLimitIsEnforcedBeforeBcrypt() throws Exception {
        String password = "汉".repeat(25);
        mvc.perform(post("/register").param("username", "bunicode").param("password", password)
                        .param("confirmPassword", password).param("displayName", "Unicode")
                        .param("email", "unicode@example.test"))
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "passwordWithinBcryptLimit"));
        assertThat(users.findByUsername("bunicode")).isEmpty();
    }

    @Test
    void authServiceAlsoValidatesWhenCalledWithoutMvc() {
        assertThatThrownBy(() -> auth.register(new RegisterForm())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> auth.updateProfile(customer.getId(), new ProfileForm("", "bad")))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void existingLongIdSessionIsAcceptedByBAndC() throws Exception {
        MockHttpSession session = sessionFor(customer);
        mvc.perform(get("/account").session(session)).andExpect(status().isOk());
        assertThat(cart.requireUserId(session)).isEqualTo(customer.getId());
    }

    @Test
    void wrongIdTypesDoNotAuthenticate() throws Exception {
        for (Object bad : new Object[]{customer.getId().toString(), 1, -1L, 0L}) {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("loginUserId", bad);
            session.setAttribute("role", "ADMIN");
            mvc.perform(get("/account").session(session)).andExpect(redirectedUrl("/login?required"));
        }
    }

    @Test
    void profileChangesOnlyCurrentUserAndPreservesIdentityHashAndRole() throws Exception {
        String hash = customer.getPasswordHash();
        MockHttpSession profileSession = sessionFor(customer);
        String profileToken = (String) mvc.perform(get("/account").session(profileSession)).andReturn()
                .getModelAndView().getModel().get("accountFormToken");
        mvc.perform(post("/account/profile").session(profileSession).param("accountFormToken", profileToken)
                        .param("displayName", "Updated name").param("email", "updated@example.test")
                        .param("fullName", "Updated Customer").param("phone", "+65 9876 5432").param("birthday", "1995-09-18")
                        .param("id", other.getId().toString()).param("userId", other.getId().toString())
                        .param("role", "ADMIN").param("username", "hacked").param("passwordHash", "hacked"))
                .andExpect(redirectedUrl("/account"));
        User saved = users.findById(customer.getId()).orElseThrow();
        assertThat(saved.getDisplayName()).isEqualTo("Updated name");
        assertThat(saved.getEmail()).isEqualTo("updated@example.test");
        assertThat(saved.getFullName()).isEqualTo("Updated Customer");
        assertThat(saved.getPhone()).isEqualTo("+65 9876 5432");
        assertThat(saved.getBirthday()).isEqualTo(java.time.LocalDate.of(1995, 9, 18));
        assertThat(saved.getRole()).isEqualTo(User.Role.CUSTOMER);
        assertThat(saved.getUsername()).isEqualTo("buser001");
        assertThat(saved.getPasswordHash()).isEqualTo(hash);
        assertThat(users.findById(other.getId()).orElseThrow().getDisplayName()).isEqualTo(other.getUsername());
    }

    @Test
    void invalidProfileDoesNotWriteAndRendersErrors() throws Exception {
        MockHttpSession profileSession = sessionFor(customer);
        String profileToken = (String) mvc.perform(get("/account").session(profileSession)).andReturn()
                .getModelAndView().getModel().get("accountFormToken");
        mvc.perform(post("/account/profile").session(profileSession).param("accountFormToken", profileToken).param("displayName", " ")
                        .param("email", "wrong"))
                .andExpect(view().name("account/view"))
                .andExpect(model().attributeHasFieldErrors("profileForm", "displayName", "email"));
        assertThat(users.findById(customer.getId()).orElseThrow().getDisplayName()).isEqualTo("buser001");
    }

    @Test
    void displayedProfileEscapesHtml() throws Exception {
        customer.setDisplayName("<script>alert(1)</script>");
        users.saveAndFlush(customer);
        mvc.perform(get("/account").session(sessionFor(customer)))
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;")));
    }

    @Test
    void customerCannotReadOrPostAdminRoutes() throws Exception {
        MockHttpSession session = login(customer.getUsername(), null);
        long count = products.count();
        mvc.perform(get("/admin/products").session(session)).andExpect(redirectedUrl("/forbidden"));
        mvc.perform(post("/admin/products").session(session).param("name", "forbidden product").param("price", "1"))
                .andExpect(redirectedUrl("/forbidden"));
        mvc.perform(post("/admin/products/" + product.getId() + "/toggle").session(session))
                .andExpect(redirectedUrl("/forbidden"));
        mvc.perform(get("/api/admin/products").session(session)).andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("ADMIN_REQUIRED"));
        assertThat(products.count()).isEqualTo(count);
        assertThat(products.findById(product.getId()).orElseThrow().isActive()).isTrue();
        mvc.perform(get("/forbidden").session(session)).andExpect(status().isForbidden());
    }

    @Test
    void realAdminLoginCanUseEPageAndModification() throws Exception {
        MockHttpSession session = login(admin.getUsername(), null);
        mvc.perform(get("/admin/products").session(session)).andExpect(status().isOk())
                .andExpect(view().name("admin/products"));
        mvc.perform(post("/admin/products/" + product.getId() + "/toggle").session(session))
                .andExpect(redirectedUrl("/admin/products?toggled"));
        assertThat(products.findById(product.getId()).orElseThrow().isActive()).isFalse();
    }

    @Test
    void databaseRoleRevocationOverridesForgedSessionRole() throws Exception {
        MockHttpSession session = login(admin.getUsername(), null);
        session.setAttribute("role", "ADMIN");
        admin.setRole(User.Role.CUSTOMER);
        users.saveAndFlush(admin);
        mvc.perform(get("/admin/products").session(session)).andExpect(redirectedUrl("/forbidden"));
        mvc.perform(get("/api/auth/session").session(session))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    @Test
    void deletedAccountInvalidatesSession() throws Exception {
        MockHttpSession session = login(customer.getUsername(), null);
        users.delete(customer);
        users.flush();
        mvc.perform(get("/account").session(session)).andExpect(redirectedUrl("/login?required"));
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void logoutInvalidatesCartAndCheckoutStateAndCannotGetLogout() throws Exception {
        MockHttpSession session = login(customer.getUsername(), null);
        cart.addItem(session, product.getId(), 2);
        checkout.prepare(session);
        mvc.perform(get("/logout").session(session)).andExpect(status().isMethodNotAllowed());
        assertThat(session.isInvalid()).isFalse();
        mvc.perform(post("/logout").session(session)).andExpect(redirectedUrl("/login?loggedOut"));
        assertThat(session.isInvalid()).isTrue();
        assertThat(LoginInterceptor.currentUserId(session)).isNull();
    }

    @Test
    void switchingLoginDropsOtherUsersCartAndCheckoutToken() throws Exception {
        MockHttpSession first = login(customer.getUsername(), null);
        cart.addItem(first, product.getId(), 2);
        String oldToken = checkout.prepare(first);
        MockHttpSession second = login(other.getUsername(), first);
        assertThat(first.isInvalid()).isTrue();
        assertThat(cart.getCartItems(second)).isEmpty();
        assertThat(checkout.prepare(second)).isNotEqualTo(oldToken);
        assertThat(second.getAttribute("loginUserId")).isEqualTo(other.getId());
    }

    @Test
    void authApiIsSafeForReactAndDoesNotExposeHash() throws Exception {
        mvc.perform(get("/api/auth/session")).andExpect(status().isOk())
                .andExpect(jsonPath("$.loggedIn").value(false)).andExpect(jsonPath("$.user").isEmpty());
        mvc.perform(get("/api/auth/session").session(login(customer.getUsername(), null)))
                .andExpect(jsonPath("$.loggedIn").value(true))
                .andExpect(jsonPath("$.user.id").value(customer.getId()))
                .andExpect(jsonPath("$.user.displayName").value(customer.getDisplayName()))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andExpect(content().string(not(containsString(customer.getPasswordHash()))))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void bLoginActuallyWorksWithCCartDCheckoutAndEHistory() throws Exception {
        MockHttpSession session = login(customer.getUsername(), null);
        // C 从本次 B 登录的 Session 读取身份和生成的表单令牌。
        MvcResult cartPage = mvc.perform(get("/cart").session(session))
                .andExpect(status().isOk()).andReturn();
        String cartToken = (String) cartPage.getModelAndView().getModel().get("cartFormToken");
        mvc.perform(post("/cart/add").session(session).param("productId", product.getId().toString())
                        .param("quantity", "2").param("cartFormToken", cartToken))
                .andExpect(redirectedUrl("/cart"));
        var checkoutPage = mvc.perform(get("/checkout").session(session))
                .andExpect(status().isOk()).andReturn();
        String token = (String) checkoutPage.getModelAndView().getModel().get("checkoutToken");
        mvc.perform(post("/checkout").session(session).param("addressId", deliveryAddress(session)).param("checkoutToken", token)
                        .param("userId", other.getId().toString()).param("totalAmount", "0.01"))
                .andExpect(redirectedUrl("/checkout/success?key=" + token));
        Order order = orders.findByCheckoutTokenAndUser_Id(token, customer.getId()).orElseThrow();
        assertThat(order.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(cart.getCartItems(session)).isEmpty();
        mvc.perform(get("/checkout/success").session(session).param("key", token))
                .andExpect(status().isOk()).andExpect(content().string(containsString("100.00")));
        mvc.perform(get("/orders").session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("100.00")));
        mvc.perform(get("/orders/" + order.getId()).session(session)).andExpect(status().isOk())
                .andExpect(content().string(containsString("B integration product")));
        mvc.perform(get("/orders/" + order.getId()).session(login(other.getUsername(), null)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/checkout/success").session(login(other.getUsername(), null)).param("key", token))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void failedDCheckoutRetainsCCartAndRetryDoesNotDuplicateOrders() throws Exception {
        MockHttpSession session = login(customer.getUsername(), null);
        cart.addItem(session, product.getId(), 1);
        String token = checkout.prepare(session);
        product.setActive(false);
        products.saveAndFlush(product);
        mvc.perform(post("/checkout").session(session).param("addressId", deliveryAddress(session)).param("checkoutToken", token))
                .andExpect(status().isOk()).andExpect(view().name("orders/checkout"));
        assertThat(orders.count()).isZero();
        assertThat(cart.countItems(session)).isEqualTo(1);
        product.setActive(true);
        products.saveAndFlush(product);
        String retryToken = checkout.prepare(session);
        mvc.perform(post("/checkout").session(session).param("addressId", deliveryAddress(session)).param("checkoutToken", retryToken))
                .andExpect(status().is3xxRedirection());
        cart.addItem(session, product.getId(), 2);
        mvc.perform(post("/checkout").session(session).param("addressId", deliveryAddress(session)).param("checkoutToken", retryToken))
                .andExpect(status().is3xxRedirection());
        assertThat(orders.count()).isEqualTo(1);
        assertThat(cart.readForCheckout(session)).containsEntry(product.getId(), 2);
    }

    @Test
    void aReactBuildAndProductRestContractRemainAvailable() throws Exception {
        mvc.perform(get("/products")).andExpect(forwardedUrl("/products/index.html"));
        mvc.perform(get("/products/index.html")).andExpect(status().isOk())
                .andExpect(content().string(containsString("type=\"module\"")));
        mvc.perform(get("/api/products").param("page", "0").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.totalPages").isNumber());
    }

    private User createUser(String username, User.Role role) {
        User user = new User(username, encoder.encode("demo123"), username, username + "@example.test");
        user.setRole(role);
        return users.saveAndFlush(user);
    }

    private MockHttpSession sessionFor(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("loginUserId", user.getId());
        return session;
    }

    private MockHttpSession login(String username, MockHttpSession previous) throws Exception {
        var request = post("/login").param("username", username).param("password", "demo123");
        if (previous != null) { request.session(previous); }
        MvcResult result = mvc.perform(request).andExpect(status().is3xxRedirection()).andReturn();
        return (MockHttpSession) result.getRequest().getSession(false);
    }
}
