package sg.edu.nus.iss.shoppingcart.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import sg.edu.nus.iss.shoppingcart.dto.AuthenticatedUser;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.service.AuthService;

import java.io.IOException;
import java.net.URI;

/**
 * 登录身份来自服务器 Session 的 Long ID；受保护请求重新核对数据库。
 * @author luopeiwen
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {
    public static final String LOGIN_USER_ID = "loginUserId";
    // 保留 E 的原有回跳属性名，避免两种约定并存。
    public static final String REDIRECT_AFTER_LOGIN = LOGIN_USER_ID + "redirectAfterLogin";
    public static final String CURRENT_USER = "currentUser";
    private final AuthService authService;

    public LoginInterceptor(AuthService authService) { this.authService = authService; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        response.setHeader("Cache-Control", "no-store");
        HttpSession session = request.getSession(false);
        Long userId = currentUserId(session);
        if (session != null && userId != null) {
            synchronized (session) {
                User user = authService.findById(userId).orElse(null);
                try {
                    // 退出或切换账号可能已使旧请求的 Session 失效。
                    if (user != null && userId.equals(currentUserId(session))) {
                        request.setAttribute(CURRENT_USER, AuthenticatedUser.from(user));
                        return true;
                    }
                    session.invalidate();
                } catch (IllegalStateException ignored) {
                    // 已失效的会话不能继续进入 Controller。
                }
            }
        } else if (session != null) {
            synchronized (session) {
                try {
                    if (session.getAttribute(LOGIN_USER_ID) != null) { session.invalidate(); }
                } catch (IllegalStateException ignored) { }
            }
        }

        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/api/")) {
            writeApiError(response, 401, "LOGIN_REQUIRED", "Please log in to continue");
            return false;
        }
        // 只记住 GET 页面，绝不在登录后重放购物车或结账 POST。
        if ("GET".equals(request.getMethod())) {
            String query = request.getQueryString();
            String original = path + (query == null ? "" : "?" + query);
            String safe = safeRedirect(original);
            if (safe != null) { request.getSession(true).setAttribute(REDIRECT_AFTER_LOGIN, safe); }
        }
        response.sendRedirect(request.getContextPath() + "/login?required");
        return false;
    }

    public static Long currentUserId(HttpSession session) {
        if (session == null) { return null; }
        try {
            Object stored = session.getAttribute(LOGIN_USER_ID);
            return stored instanceof Long id && id > 0 ? id : null;
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    /**
 * 仅由验证密码后的登录 Controller 在新建的 Session 上调用。 */
    public static void establishSession(HttpSession session, User user) {
        if (user == null || user.getId() == null || user.getId() <= 0) {
            throw new IllegalArgumentException("A persisted user is required");
        }
        session.setAttribute(LOGIN_USER_ID, user.getId());
    }

    public static void writeApiError(HttpServletResponse response, int status,
                                     String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"error\":\"" + error + "\",\"message\":\"" + message + "\"}");
    }

    /**
 * 仅允许本应用内的已知页面，拒绝外链、编码路径及路径穿越。 */
    public static String safeRedirect(String candidate) {
        if (candidate == null || candidate.isBlank() || candidate.length() > 2048
                || candidate.chars().anyMatch(c -> c < 32 || c == 127)
                || candidate.indexOf('\\') >= 0) { return null; }
        try {
            URI uri = URI.create(candidate);
            String path = uri.getPath();
            if (uri.isAbsolute() || uri.getRawAuthority() != null || uri.getFragment() != null
                    || path == null || !path.startsWith("/") || path.startsWith("//")
                    || !uri.getRawPath().equals(path) || !uri.normalize().getPath().equals(path)
                    || path.indexOf(';') >= 0 || path.indexOf('\\') >= 0) { return null; }
            boolean allowed = path.equals("/cart") || path.equals("/cart/products")
                    || path.equals("/checkout") || path.equals("/checkout/success")
                    || path.equals("/account") || path.equals("/orders")
                    || path.equals("/products") || path.equals("/products/")
                    || path.equals("/account/addresses") || path.equals("/account/addresses/new")
                    || path.matches("/account/addresses/[0-9]+/edit")
                    || path.matches("/orders/[0-9]+")
                    || path.equals("/admin") || path.startsWith("/admin/");
            return allowed ? candidate : null;
        } catch (IllegalArgumentException ex) { return null; }
    }
}
