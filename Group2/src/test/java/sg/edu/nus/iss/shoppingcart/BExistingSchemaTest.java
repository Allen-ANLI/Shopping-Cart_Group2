package sg.edu.nus.iss.shoppingcart;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.repository.OrderRepository;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import sg.edu.nus.iss.shoppingcart.service.AuthService;
import sg.edu.nus.iss.shoppingcart.service.CartService;
import sg.edu.nus.iss.shoppingcart.service.CheckoutCoordinator;

import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 用 A 的旧 app_users 表结构和已有哈希账号验证增量接入，不重建旧用户。
 * H2 兼容模式不替代正式 MySQL 联调。
 * @author luopeiwen
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:b_existing_schema;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=update"
})
@AutoConfigureMockMvc
@ContextConfiguration(initializers = BExistingSchemaTest.LegacySchemaInitializer.class)
class BExistingSchemaTest {
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

    @Autowired AuthService auth;
    @Autowired MockMvc mvc;
    @Autowired CartService cart;
    @Autowired CheckoutCoordinator checkout;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;

    static class LegacySchemaInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            try (var connection = DriverManager.getConnection(
                    "jdbc:h2:mem:b_existing_schema;MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", "")) {
                try (var statement = connection.createStatement()) {
                    statement.execute("""
                            CREATE TABLE app_users (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                username VARCHAR(50) NOT NULL UNIQUE,
                                password_hash VARCHAR(255) NOT NULL,
                                display_name VARCHAR(100) NOT NULL)
                            """);
                }
                try (var statement = connection.prepareStatement(
                        "INSERT INTO app_users(id,username,password_hash,display_name) VALUES(?,?,?,?)")) {
                    statement.setLong(1, 41L);
                    statement.setString(2, "legacyalice");
                    statement.setString(3, new BCryptPasswordEncoder().encode("demo123"));
                    statement.setString(4, "Existing A user");
                    statement.executeUpdate();
                }
            } catch (Exception ex) {
                throw new IllegalStateException("Cannot initialize isolated legacy schema", ex);
            }
        }
    }

    @Test
    void oldUserIdAndBcryptHashRemainUsableAfterAddingRoleAndEmail() {
        User user = auth.authenticate("legacyalice", "demo123").orElseThrow();
        assertThat(user.getId()).isEqualTo(41L);
        assertThat(user.getDisplayName()).isEqualTo("Existing A user");
        assertThat(user.getRole()).isEqualTo(User.Role.CUSTOMER);
        assertThat(user.getEmail()).isNull();
    }

    @Test
    void oldAUserCanLogInAndPlaceDOrderWithUnchangedForeignKey() throws Exception {
        var result = mvc.perform(post("/login").param("username", "legacyalice").param("password", "demo123"))
                .andExpect(redirectedUrl("/products")).andReturn();
        var session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session.getAttribute("loginUserId")).isEqualTo(41L);
        mvc.perform(get("/cart").session(session)).andExpect(status().isOk());
        var product = products.findAll().get(0);
        cart.addItem(session, product.getId(), 1);
        String token = checkout.prepare(session);
        mvc.perform(post("/checkout").param("paymentMethod", "VISA").param("cardholderName", "Demo Customer")
                .param("cardNumber", "4242424242424242").param("cardExpiry", "12/99")
                .param("cardSecurityCode", "123").param("paymentPin", "123456").session(session).param("addressId", deliveryAddress(session)).param("checkoutToken", token))
                .andExpect(redirectedUrlPattern("/orders/*/payment"));
        assertThat(orders.findByCheckoutTokenAndUser_Id(token, 41L)).isPresent();
        assertThat(auth.findById(41L)).isPresent();
    }
}
