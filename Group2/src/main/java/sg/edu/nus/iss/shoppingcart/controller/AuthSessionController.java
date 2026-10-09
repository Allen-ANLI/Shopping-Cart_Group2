package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sg.edu.nus.iss.shoppingcart.dto.AuthenticatedUser;
import sg.edu.nus.iss.shoppingcart.interceptor.LoginInterceptor;
import sg.edu.nus.iss.shoppingcart.service.AuthService;
import sg.edu.nus.iss.shoppingcart.service.CartService;

/**
 * 给 Angular 提供安全的登录状态；不返回 passwordHash 或 User 实体。
 * @author luopeiwen
 * @author 王重一 UI 导航与操作优化
 */
@RestController
public class AuthSessionController {
    private final AuthService authService;
    private final CartService cart;
    public AuthSessionController(AuthService authService, CartService cart) {
        this.authService = authService; this.cart = cart;
    }

    public record LoginState(boolean loggedIn, AuthenticatedUser user, int cartQuantity) {}

    @GetMapping("/api/auth/session")
    public LoginState session(HttpServletRequest request, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        Long id = LoginInterceptor.currentUserId(request.getSession(false));
        var user = authService.findById(id).map(AuthenticatedUser::from).orElse(null);
        int quantity = user == null ? 0 : cart.countTotalQuantity(cart.getCartItems(request.getSession(false)));
        return new LoginState(user != null, user, quantity);
    }
}
