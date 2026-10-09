package sg.edu.nus.iss.shoppingcart.interceptor;

import jakarta.servlet.http.HttpSession;

/**
 * 身份来自 B 已认证的 Session，不接受请求中的 userId。
 * @author Letian Xie
 */
public final class CartSessionIdentity {
    private CartSessionIdentity() {}

    public static Long currentUserId(HttpSession session) {
        if (session == null) { return null; }
        try {
            Object stored = session.getAttribute("loginUserId");
            return stored instanceof Long id && id > 0 ? id : null;
        } catch (IllegalStateException ex) {
            return null;
        }
    }
}
