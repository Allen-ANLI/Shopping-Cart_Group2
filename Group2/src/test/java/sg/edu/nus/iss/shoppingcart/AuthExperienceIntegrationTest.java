package sg.edu.nus.iss.shoppingcart;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.interceptor.LoginInterceptor;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;
import sg.edu.nus.iss.shoppingcart.service.AuthService;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class AuthExperienceIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired AuthService auth;
    User member;
    String suffix;

    @BeforeEach
    void setup() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        member = new User("auth" + suffix, encoder.encode("Member123!"), "Member", suffix + "@example.test");
        member.setPhone("+65 9123 4567");
        member = users.saveAndFlush(member);
    }

    @ParameterizedTest
    @ValueSource(strings = {"password!", "PASSWORD!", "Password1", "Aa!1234"})
    void registrationRejectsEachMissingPasswordRequirement(String password) throws Exception {
        mvc.perform(registration(password).cookie(new Cookie("store_lang", "en")))
                .andExpect(status().isOk()).andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerForm", "password"));
        assertThat(users.findByUsername("join" + suffix)).isEmpty();
    }

    @Test
    void registrationNeedsNoFullNameAndSupportsAllThreeLoginMethods() throws Exception {
        mvc.perform(registration("SecurePass!")).andExpect(redirectedUrl("/login?registered"));
        User registered = users.findByUsername("join" + suffix).orElseThrow();
        assertThat(registered.getFullName()).isNull();
        assertThat(encoder.matches("SecurePass!", registered.getPasswordHash())).isTrue();
        for (String[] login : new String[][]{{"username", registered.getUsername().toUpperCase()},
                {"email", registered.getEmail().toUpperCase()}, {"phone", "6598887766"}}) {
            var result = mvc.perform(post("/login").param("loginMethod", login[0])
                            .param("username", login[1]).param("password", "SecurePass!"))
                    .andExpect(redirectedUrl("/products")).andReturn();
            assertThat(result.getRequest().getSession(false).getAttribute(LoginInterceptor.LOGIN_USER_ID))
                    .isEqualTo(registered.getId());
        }
    }

    @Test
    void selectedLanguageControlsServerAndClientValidationMessages() throws Exception {
        mvc.perform(registration("weakpass").cookie(new Cookie("store_lang", "en")))
                .andExpect(content().string(containsString("Password must contain an uppercase letter")))
                .andExpect(content().string(not(containsString("密码必须"))));
        mvc.perform(registration("weakpass").cookie(new Cookie("store_lang", "zh")))
                .andExpect(content().string(containsString("密码必须包含大写字母、小写字母和特殊字符")))
                .andExpect(content().string(not(containsString("Password must contain an uppercase letter"))));
        mvc.perform(get("/register").cookie(new Cookie("store_lang", "en")))
                .andExpect(content().string(containsString("novalidate")))
                .andExpect(content().string(containsString("data-required-message=\"Email is required.\"")))
                .andExpect(content().string(not(containsString("id=\"fullName\""))));
        mvc.perform(post("/register").cookie(new Cookie("store_lang", "zh"))
                        .param("birthday", "not-a-date"))
                .andExpect(content().string(containsString("请以 YYYY-MM-DD 格式填写有效生日")));
    }

    @Test
    void loginScreenHasMethodChoicesAndNoDemoCredentials() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(content().string(containsString("name=\"loginMethod\"")))
                .andExpect(content().string(containsString("value=\"email\"")))
                .andExpect(content().string(containsString("value=\"phone\"")))
                .andExpect(content().string(not(containsString("demo123"))))
                .andExpect(content().string(not(containsString("admin123"))));
        mvc.perform(post("/login").cookie(new Cookie("store_lang", "en"))
                        .param("loginMethod", "email").param("username", member.getEmail()).param("password", "wrong"))
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(containsString("The login details or password are incorrect")));
        assertThat(auth.authenticate("phone", "+65 (9123)-4567", "Member123!")).contains(member);
        assertThat(auth.authenticate("email", member.getUsername(), "Member123!")).isEmpty();
        assertThat(auth.authenticate("invalid", member.getUsername(), "Member123!")).isEmpty();
    }

    @Test
    void duplicateContactsAreRejectedAndAmbiguousLegacyContactsCannotLogIn() throws Exception {
        mvc.perform(registration("SecurePass!").with(request -> {
                    request.setParameter("email", member.getEmail().toUpperCase()); return request;
                }))
                .andExpect(view().name("auth/register"))
                .andExpect(content().string(containsString("email address is already linked")));
        mvc.perform(registration("SecurePass!").with(request -> {
                    request.setParameter("phone", "65 (9123)-4567"); return request;
                }))
                .andExpect(view().name("auth/register"))
                .andExpect(content().string(containsString("phone number is already linked")));
        User legacy = new User("legacy" + suffix, encoder.encode("Member123!"), "Legacy", member.getEmail());
        legacy.setPhone("6591234567");
        users.saveAndFlush(legacy);
        assertThat(auth.authenticate("email", member.getEmail(), "Member123!")).isEmpty();
        assertThat(auth.authenticate("phone", "+65 9123 4567", "Member123!")).isEmpty();
        assertThat(auth.authenticate(member.getUsername(), "Member123!")).contains(member);
    }

    @Test
    void profileCanUpdateContactWithoutFullNameAndLocalizesDuplicateError() throws Exception {
        var session = new MockHttpSession();
        session.setAttribute(LoginInterceptor.LOGIN_USER_ID, member.getId());
        String token = (String) mvc.perform(get("/account").session(session)).andReturn()
                .getModelAndView().getModel().get("accountFormToken");
        mvc.perform(post("/account/profile").session(session).param("accountFormToken", token)
                        .param("displayName", "Updated member").param("email", "updated" + suffix + "@example.test")
                        .param("phone", "+65 8111 2233"))
                .andExpect(redirectedUrl("/account"));
        assertThat(auth.authenticate("phone", "6581112233", "Member123!")).isPresent();
        assertThat(auth.authenticate("phone", "6591234567", "Member123!")).isEmpty();
        User other = new User("other" + suffix, encoder.encode("Member123!"), "Other", "other" + suffix + "@example.test");
        users.saveAndFlush(other);
        mvc.perform(post("/account/profile").session(session).cookie(new Cookie("store_lang", "zh"))
                        .param("accountFormToken", token).param("displayName", "Updated member")
                        .param("email", other.getEmail()).param("phone", "+65 8111 2233"))
                .andExpect(view().name("account/view"))
                .andExpect(content().string(containsString("此邮箱已绑定其他账户")));
    }

    private MockHttpServletRequestBuilder registration(String password) {
        return post("/register").param("username", "join" + suffix).param("password", password)
                .param("confirmPassword", password).param("displayName", "New member")
                .param("email", "join" + suffix + "@example.test").param("phone", "+65 9888 7766");
    }
}
