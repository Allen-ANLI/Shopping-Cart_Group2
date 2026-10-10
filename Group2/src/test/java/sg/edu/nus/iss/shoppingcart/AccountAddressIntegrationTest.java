package sg.edu.nus.iss.shoppingcart;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.form.ShippingAddressForm;
import sg.edu.nus.iss.shoppingcart.interceptor.LoginInterceptor;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;
import sg.edu.nus.iss.shoppingcart.service.ShippingAddressService;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class AccountAddressIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired ShippingAddressService addresses;
    @Autowired PasswordEncoder encoder;
    @Autowired EntityManager entityManager;
    User owner;
    User other;

    @BeforeEach
    void setup() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        owner = users.saveAndFlush(new User("owner" + suffix, encoder.encode("testpass123"), "Owner", "owner@example.test"));
        other = users.saveAndFlush(new User("other" + suffix, encoder.encode("testpass123"), "Other", "other@example.test"));
    }

    @Test
    void profileChangesAreLoadedFromDatabaseInANewLoginSession() throws Exception {
        MockHttpSession session = sessionFor(owner);
        String token = token(session);
        mvc.perform(post("/account/profile").session(session).param("accountFormToken", token)
                        .param("fullName", "Lin Mei").param("displayName", "Mei")
                        .param("email", "mei@example.test").param("phone", "+65 8123 4567")
                        .param("birthday", "1995-04-03"))
                .andExpect(redirectedUrl("/account"));
        entityManager.flush();
        entityManager.clear();
        User reloaded = users.findById(owner.getId()).orElseThrow();
        assertThat(reloaded.getFullName()).isEqualTo("Lin Mei");
        assertThat(reloaded.getPhone()).isEqualTo("+65 8123 4567");
        assertThat(reloaded.getBirthday()).isEqualTo(LocalDate.of(1995, 4, 3));
        mvc.perform(post("/logout").session(session)).andExpect(status().is3xxRedirection());
        var login = mvc.perform(post("/login").param("username", owner.getUsername()).param("password", "testpass123"))
                .andExpect(status().is3xxRedirection()).andReturn();
        MockHttpSession fresh = (MockHttpSession) login.getRequest().getSession(false);
        mvc.perform(get("/account").session(fresh)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Lin Mei")))
                .andExpect(content().string(containsString("+65 8123 4567")))
                .andExpect(content().string(containsString("1995-04-03")));
    }

    @Test
    void addressCreateEditDefaultAndDeletePersistAndKeepOneDefault() throws Exception {
        MockHttpSession session = sessionFor(owner);
        String token = token(session);
        mvc.perform(post("/account/addresses").session(session).param("accountFormToken", token)
                        .param("recipientName", "Lin Mei").param("phone", "+65 8123 4567")
                        .param("addressLine1", "10 Orchard Road").param("addressLine2", "#03-01")
                        .param("city", "Singapore").param("country", "Singapore").param("postalCode", "238823")
                        .param("userId", other.getId().toString()).param("returnTo", "/checkout"))
                .andExpect(redirectedUrl("/checkout"));
        entityManager.flush(); entityManager.clear();
        var first = addresses.listForUser(owner.getId()).get(0);
        assertThat(first.isDefaultAddress()).isTrue();
        assertThat(addresses.listForUser(other.getId())).isEmpty();
        var second = addresses.saveForUser(owner.getId(), null, address("20 Clementi Road"));
        mvc.perform(post("/account/addresses/" + second.getId() + "/default").session(session)
                        .param("accountFormToken", token)).andExpect(redirectedUrl("/account/addresses"));
        entityManager.flush(); entityManager.clear();
        assertThat(addresses.listForUser(owner.getId()).stream().filter(a -> a.isDefaultAddress()).map(a -> a.getId()))
                .containsExactly(second.getId());
        // An unchecked box on the existing default must not leave the account without a default.
        addresses.saveForUser(owner.getId(), second.getId(), address("22 Clementi Road"));
        entityManager.flush(); entityManager.clear();
        assertThat(addresses.requireForUser(second.getId(), owner.getId()).isDefaultAddress()).isTrue();
        mvc.perform(post("/account/addresses/" + second.getId() + "/delete").session(session)
                        .param("accountFormToken", token)).andExpect(redirectedUrl("/account/addresses"));
        entityManager.flush(); entityManager.clear();
        assertThat(addresses.listForUser(owner.getId())).hasSize(1);
        assertThat(addresses.requireForUser(first.getId(), owner.getId()).isDefaultAddress()).isTrue();
    }

    @Test
    void addressOwnershipIsEnforcedForReadEditDeleteAndDefault() throws Exception {
        Long addressId = addresses.saveForUser(owner.getId(), null, address("Private owner address")).getId();
        entityManager.flush(); entityManager.clear();
        MockHttpSession attacker = sessionFor(other);
        String token = token(attacker);
        mvc.perform(get("/account/addresses/" + addressId + "/edit").session(attacker))
                .andExpect(status().isNotFound()).andExpect(content().string(not(containsString("Private owner address"))));
        mvc.perform(post("/account/addresses/" + addressId).session(attacker).param("accountFormToken", token)
                        .param("recipientName", "Attacker").param("phone", "+65 8888 8888")
                        .param("country", "Singapore").param("city", "Singapore").param("postalCode", "123456")
                        .param("addressLine1", "Attacker street"))
                .andExpect(status().isNotFound());
        for (String action : new String[]{"delete", "default"}) {
            mvc.perform(post("/account/addresses/" + addressId + "/" + action).session(attacker)
                            .param("accountFormToken", token)).andExpect(status().isNotFound());
        }
        assertThatThrownBy(() -> addresses.requireForUser(addressId, other.getId())).isInstanceOf(ResponseStatusException.class);
        assertThat(addresses.requireForUser(addressId, owner.getId()).getAddressLine1()).isEqualTo("Private owner address");
    }

    @Test
    void personalDataChangesRequireASessionBoundFormToken() throws Exception {
        MockHttpSession session = sessionFor(owner);
        String otherToken = token(sessionFor(other));
        mvc.perform(post("/account/profile").session(session).param("displayName", "Injected"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/account/addresses").session(session).param("accountFormToken", otherToken))
                .andExpect(status().isForbidden());
        Long id = addresses.saveForUser(owner.getId(), null, address("Saved address")).getId();
        mvc.perform(post("/account/addresses/" + id + "/delete").session(session)
                        .param("accountFormToken", "forged")).andExpect(status().isForbidden());
        assertThat(addresses.listForUser(owner.getId())).hasSize(1);
        assertThat(users.findById(owner.getId()).orElseThrow().getDisplayName()).isEqualTo("Owner");
    }

    @Test
    void addressValidationAndReturnPathStayWithinTheApplication() throws Exception {
        MockHttpSession session = sessionFor(owner);
        String token = token(session);
        mvc.perform(post("/account/addresses").session(session).param("accountFormToken", token)
                        .param("recipientName", " ").param("phone", "not-phone"))
                .andExpect(view().name("account/address-form"))
                .andExpect(model().attributeHasFieldErrors("addressForm", "recipientName", "phone", "country", "city", "postalCode", "addressLine1"));
        assertThat(addresses.listForUser(owner.getId())).isEmpty();
        mvc.perform(post("/account/addresses").session(session).param("accountFormToken", token)
                        .param("recipientName", "Lin Mei").param("phone", "+65 8123 4567")
                        .param("addressLine1", "10 Orchard Road").param("city", "Singapore")
                        .param("country", "Singapore").param("postalCode", "238823")
                        .param("returnTo", "https://example.com/"))
                .andExpect(redirectedUrl("/account/addresses"));
    }

    @Test
    void chineseModeTranslatesAccountFormsAndValidation() throws Exception {
        MockHttpSession session = sessionFor(owner);
        mvc.perform(get("/account").session(session).cookie(new jakarta.servlet.http.Cookie("store_lang", "zh")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("个人资料")));
        mvc.perform(get("/account/addresses/new").session(session).cookie(new jakarta.servlet.http.Cookie("store_lang", "zh")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("收件人姓名")));
        mvc.perform(post("/register").cookie(new jakarta.servlet.http.Cookie("store_lang", "zh")).param("username", "validuser").param("password", "testpass123")
                        .param("confirmPassword", "testpass123").param("displayName", "测试").param("email", "test@example.test"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "fullName", "phone"))
                .andExpect(content().string(containsString("请填写真实姓名")));
    }

    private MockHttpSession sessionFor(User user) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(LoginInterceptor.LOGIN_USER_ID, user.getId());
        return session;
    }
    private String token(MockHttpSession session) throws Exception {
        return (String) mvc.perform(get("/account").session(session)).andExpect(status().isOk()).andReturn()
                .getModelAndView().getModel().get("accountFormToken");
    }
    private ShippingAddressForm address(String street) {
        ShippingAddressForm form = new ShippingAddressForm();
        form.setRecipientName("Lin Mei"); form.setPhone("+65 8123 4567");
        form.setCountry("Singapore"); form.setCity("Singapore"); form.setPostalCode("123456");
        form.setAddressLine1(street);
        return form;
    }
}
