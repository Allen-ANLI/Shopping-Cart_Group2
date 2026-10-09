package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sg.edu.nus.iss.shoppingcart.dto.AuthenticatedUser;
import sg.edu.nus.iss.shoppingcart.interceptor.LoginInterceptor;
import sg.edu.nus.iss.shoppingcart.service.AuthService;

/**
 * 给 Angular 提供安全的登录状态；不返回 passwordHash 或 User 实体。
 * @author luopeiwen
 */
@RestController
public class AuthSessionController {
    private final AuthService authService;
    public AuthSessionController(AuthService authService) { this.authService = authService; }

    public record LoginState(boolean loggedIn, AuthenticatedUser user) {}

    @GetMapping("/api/auth/session")
    public LoginState session(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        Long id = LoginInterceptor.currentUserId(request.getSession(false));
        var user = authService.findById(id).map(AuthenticatedUser::from).orElse(null);
        return new LoginState(user != null, user);
    }
}
