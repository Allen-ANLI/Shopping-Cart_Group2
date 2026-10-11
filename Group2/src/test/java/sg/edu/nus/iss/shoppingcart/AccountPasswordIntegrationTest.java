package sg.edu.nus.iss.shoppingcart;

import jakarta.servlet.http.Cookie;
import java.util.UUID;
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
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.form.PasswordChangeForm;
import sg.edu.nus.iss.shoppingcart.interceptor.LoginInterceptor;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;
import sg.edu.nus.iss.shoppingcart.service.AccountFormTokens;
import sg.edu.nus.iss.shoppingcart.service.AuthService;

import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("h2")
@Transactional
class AccountPasswordIntegrationTest {
    private static final String OLD_PASSWORD = "demo123";
    private static final String NEW_PASSWORD = "UpdatedAccount42!";
    @Autowired MockMvc mvc;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder encoder;
    @Autowired AuthService auth;
    User member, other;
    MockHttpSession session;
    String token, oldHash;

    @BeforeEach void setUp() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        member = users.saveAndFlush(new User("pwd" + suffix, encoder.encode(OLD_PASSWORD), "Member", suffix + "@example.test"));
        other = users.saveAndFlush(new User("other" + suffix, encoder.encode(OLD_PASSWORD), "Other", "other" + suffix + "@example.test"));
        oldHash = member.getPasswordHash();
        session = new MockHttpSession();
        session.setAttribute(LoginInterceptor.LOGIN_USER_ID, member.getId());
        token = AccountFormTokens.get(session);
    }

    @Test void accountOffersPasswordSettingsWithoutExposingStoredCredentials() throws Exception {
        mvc.perform(get("/account").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("passwordChangeForm"))
                .andExpect(content().string(containsString("id=\"password-settings\"")))
                .andExpect(content().string(containsString("autocomplete=\"current-password\"")))
                .andExpect(content().string(containsString("data-password-target=\"newPassword\"")))
                .andExpect(content().string(not(containsString(oldHash))))
                .andExpect(content().string(not(containsString(OLD_PASSWORD))));
    }

    @Test void verifiedChangeUpdatesOnlyCurrentAccountAndRotatesSessionWithoutLosingState() throws Exception {
        String previousSessionId = session.getId();
        session.setAttribute("passwordChangeStateProbe", "retained");
        mvc.perform(change(OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD)
                        .param("userId", other.getId().toString()).param("role", "ADMIN")
                        .param("passwordHash", "untrusted"))
                .andExpect(redirectedUrl("/account#password-settings"))
                .andExpect(flash().attribute("passwordChanged", true));
        assertThat(session.getId()).isNotEqualTo(previousSessionId);
        assertThat(session.getAttribute("passwordChangeStateProbe")).isEqualTo("retained");
        assertThat(LoginInterceptor.currentUserId(session)).isEqualTo(member.getId());
        assertThat(member.getPasswordHash()).isNotEqualTo(oldHash).isNotEqualTo(NEW_PASSWORD);
        assertThat(encoder.matches(NEW_PASSWORD, member.getPasswordHash())).isTrue();
        assertThat(member.getRole()).isEqualTo(User.Role.CUSTOMER);
        assertThat(encoder.matches(OLD_PASSWORD, other.getPasswordHash())).isTrue();
        assertThat(auth.authenticate(member.getUsername(), OLD_PASSWORD)).isEmpty();
        assertThat(auth.authenticate(member.getUsername(), NEW_PASSWORD)).isPresent();
        assertThat(auth.authenticate("email", member.getEmail(), NEW_PASSWORD)).isPresent();
    }

    @ParameterizedTest @ValueSource(strings = {"en", "zh"})
    void wrongCurrentPasswordIsLocalizedAndNeverEchoed(String language) throws Exception {
        mvc.perform(change("incorrect-original", NEW_PASSWORD, NEW_PASSWORD).cookie(new Cookie("store_lang", language)))
                .andExpect(view().name("account/view"))
                .andExpect(model().attributeHasErrors("passwordChangeForm"))
                .andExpect(content().string(containsString(language.equals("zh") ? "原密码不正确" : "Your current password is incorrect")))
                .andExpect(content().string(not(containsString("incorrect-original"))))
                .andExpect(content().string(not(containsString(NEW_PASSWORD))))
                .andExpect(content().string(not(containsString(oldHash))));
        assertUnchanged();
    }

    @ParameterizedTest @ValueSource(strings = {"password!", "PASSWORD!", "Password1", "Aa!1234"})
    void newPasswordMustMeetAllRegistrationRules(String weakPassword) throws Exception {
        mvc.perform(change(OLD_PASSWORD, weakPassword, weakPassword))
                .andExpect(view().name("account/view"))
                .andExpect(model().attributeHasFieldErrors("passwordChangeForm", "newPassword"));
        assertUnchanged();
    }

    @ParameterizedTest @ValueSource(strings = {"currentPassword", "newPassword", "confirmPassword"})
    void allThreePasswordInputsAreRequired(String field) throws Exception {
        mvc.perform(change(OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD).with(request -> {
                    request.setParameter(field, ""); return request;
                }))
                .andExpect(view().name("account/view"))
                .andExpect(model().attributeHasFieldErrors("passwordChangeForm", field));
        assertUnchanged();
    }

    @Test void confirmationMismatchDoesNotChangeThePassword() throws Exception {
        mvc.perform(change(OLD_PASSWORD, NEW_PASSWORD, "DifferentPassword!"))
                .andExpect(model().attributeHasFieldErrors("passwordChangeForm", "passwordConfirmed"))
                .andExpect(content().string(containsString("Passwords do not match.")));
        assertUnchanged();
    }

    @Test void characterAndBcryptByteLimitsAreEnforcedBeforeEncoding() throws Exception {
        for (String password : new String[]{"Aa!" + "a".repeat(48), "Aa!" + "界".repeat(24)}) {
            mvc.perform(change(OLD_PASSWORD, password, password))
                    .andExpect(view().name("account/view"))
                    .andExpect(model().attributeHasErrors("passwordChangeForm"));
        }
        mvc.perform(change("界".repeat(25), NEW_PASSWORD, NEW_PASSWORD))
                .andExpect(model().attributeHasFieldErrors("passwordChangeForm", "passwordWithinBcryptLimit"));
        assertUnchanged();
    }

    @Test void newPasswordCannotReuseTheCurrentPassword() throws Exception {
        member.setPasswordHash(encoder.encode(NEW_PASSWORD)); users.saveAndFlush(member);
        mvc.perform(change(NEW_PASSWORD, NEW_PASSWORD, NEW_PASSWORD).cookie(new Cookie("store_lang", "zh")))
                .andExpect(view().name("account/view"))
                .andExpect(content().string(containsString("新密码不能与原密码相同")));
        assertThat(encoder.matches(NEW_PASSWORD, member.getPasswordHash())).isTrue();
    }

    @Test void changingPasswordRequiresLoginAndTheCurrentSessionsFormToken() throws Exception {
        mvc.perform(post("/account/password").param("currentPassword", OLD_PASSWORD)
                        .param("newPassword", NEW_PASSWORD).param("confirmPassword", NEW_PASSWORD))
                .andExpect(redirectedUrl("/login?required"));
        for (String badToken : new String[]{"", "not-a-valid-token", AccountFormTokens.get(new MockHttpSession())}) {
            mvc.perform(change(OLD_PASSWORD, NEW_PASSWORD, NEW_PASSWORD).with(request -> {
                        request.setParameter("accountFormToken", badToken); return request;
                    })).andExpect(status().isForbidden());
        }
        assertUnchanged();
    }

    @Test void passwordRulesAreAlsoEnforcedByTheService() {
        var form = new PasswordChangeForm();
        form.setCurrentPassword(OLD_PASSWORD); form.setNewPassword("weakpass"); form.setConfirmPassword("weakpass");
        assertThatThrownBy(() -> auth.changePassword(member.getId(), form)).isInstanceOf(BusinessException.class);
        assertUnchanged();
    }

    private MockHttpServletRequestBuilder change(String current, String next, String confirmation) {
        return post("/account/password").session(session).param("accountFormToken", token)
                .param("currentPassword", current).param("newPassword", next).param("confirmPassword", confirmation);
    }
    private void assertUnchanged() { assertThat(users.findById(member.getId()).orElseThrow().getPasswordHash()).isEqualTo(oldHash); }
}
