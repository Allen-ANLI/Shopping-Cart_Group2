package sg.edu.nus.iss.shoppingcart.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.i18n.LocaleContextHolder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/** Session-bound anti-forgery tokens for personal information and address changes. */
public final class AccountFormTokens {
    public static final String ATTRIBUTE = "accountFormToken";
    private AccountFormTokens() { }
    public static String get(HttpSession session) {
        synchronized (session) {
            Object existing = session.getAttribute(ATTRIBUTE);
            if (existing instanceof String token) { return token; }
            String token = UUID.randomUUID().toString();
            session.setAttribute(ATTRIBUTE, token);
            return token;
        }
    }
    public static void require(HttpSession session, String submitted) {
        Object existing = session.getAttribute(ATTRIBUTE);
        if (!(existing instanceof String expected) || submitted == null
                || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), submitted.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "zh".equals(LocaleContextHolder.getLocale().getLanguage())
                            ? "请刷新表单后重试。" : "Please reload the form and try again.");
        }
    }
}
