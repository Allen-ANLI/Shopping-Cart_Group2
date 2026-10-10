package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import sg.edu.nus.iss.shoppingcart.dto.AuthenticatedUser;
import sg.edu.nus.iss.shoppingcart.interceptor.LoginInterceptor;
import sg.edu.nus.iss.shoppingcart.service.AuthService;
import sg.edu.nus.iss.shoppingcart.service.AvatarService;
import sg.edu.nus.iss.shoppingcart.service.CartService;

/**
 * 统一页面登录状态；角色不从客户端或 Session 缓存读取。
 * @author luopeiwen
 */
@ControllerAdvice
public class AuthModelAdvice {
    private final AuthService authService;
    private final Environment environment;
    private final CartService cartService;
    private final AvatarService avatars;
    public AuthModelAdvice(AuthService authService, Environment environment, CartService cartService,
                           AvatarService avatars) {
        this.authService = authService;
        this.environment = environment;
        this.cartService = cartService;
        this.avatars = avatars;
    }

    @ModelAttribute
    public void identity(HttpServletRequest request, HttpServletResponse response, Model model) {
        response.setHeader("Cache-Control", "no-store");
        Object checked = request.getAttribute(LoginInterceptor.CURRENT_USER);
        AuthenticatedUser user = checked instanceof AuthenticatedUser value ? value
                : authService.findById(LoginInterceptor.currentUserId(request.getSession(false)))
                        .map(AuthenticatedUser::from).orElse(null);
        String path = request.getRequestURI();
        String activeNav = path.startsWith("/admin") ? "admin" : path.startsWith("/account") ? "account"
                : path.startsWith("/orders") || path.startsWith("/checkout/success") ? "orders"
                : path.startsWith("/cart") || path.startsWith("/checkout") ? "cart"
                : path.startsWith("/login") ? "login" : path.startsWith("/register") ? "register" : "products";
        model.addAttribute("activeNav", activeNav);
        model.addAttribute("currentUser", user);
        model.addAttribute("currentAvatarUrl", user != null && avatars.exists(user.id()) ? "/api/account/avatar" : null);
        model.addAttribute("isLoggedIn", user != null);
        model.addAttribute("isAdmin", user != null && user.isAdmin());
        model.addAttribute("navCartQuantity", user == null ? 0 : cartService.quantities(request.getSession()).values().stream().mapToInt(Integer::intValue).sum());
        model.addAttribute("showDemoAccounts", environment.acceptsProfiles(Profiles.of("b-demo")));
    }
}
