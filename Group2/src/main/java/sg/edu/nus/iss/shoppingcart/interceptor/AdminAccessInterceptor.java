package sg.edu.nus.iss.shoppingcart.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import sg.edu.nus.iss.shoppingcart.dto.AuthenticatedUser;
import sg.edu.nus.iss.shoppingcart.service.AuthService;

/**
 * 沿用 E 的类名和路由，由 B 提供真实身份；不保留两套管理员拦截器。
 * @author 蔡千一 (Module E original guard)
 * @author luopeiwen (B integration)
 */
@Component
public class AdminAccessInterceptor implements HandlerInterceptor {
    private final AuthService authService;
    public AdminAccessInterceptor(AuthService authService) { this.authService = authService; }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws java.io.IOException {
        Object checked = request.getAttribute(LoginInterceptor.CURRENT_USER);
        AuthenticatedUser user = checked instanceof AuthenticatedUser value ? value
                : authService.findById(LoginInterceptor.currentUserId(request.getSession(false)))
                        .map(AuthenticatedUser::from).orElse(null);
        if (user != null && user.isAdmin()) { return true; }
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith("/api/")) {
            LoginInterceptor.writeApiError(response, user == null ? 401 : 403,
                    user == null ? "LOGIN_REQUIRED" : "ADMIN_REQUIRED",
                    user == null ? "Please log in to continue" : "Administrator access is required");
        } else {
            response.sendRedirect(request.getContextPath()
                    + (user == null ? "/login?required" : "/forbidden"));
        }
        return false;
    }
}
