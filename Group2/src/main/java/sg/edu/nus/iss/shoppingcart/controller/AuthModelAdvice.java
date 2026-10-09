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
import sg.edu.nus.iss.shoppingcart.service.CartService;

/**
 * 统一页面登录状态；角色不从客户端或 Session 缓存读取。
 * @author luopeiwen
 * @author 王重一 UI 导航与操作优化
 */
@ControllerAdvice
public class AuthModelAdvice {
    private final AuthService authService;
    private final Environment environment;
    private final CartService cart;
    public AuthModelAdvice(AuthService authService, Environment environment, CartService cart) {
        this.authService = authService;
        this.environment = environment;
        this.cart = cart;
    }

    @ModelAttribute
    public void identity(HttpServletRequest request, HttpServletResponse response, Model model) {
        response.setHeader("Cache-Control", "no-store");
        Object checked = request.getAttribute(LoginInterceptor.CURRENT_USER);
        AuthenticatedUser user = checked instanceof AuthenticatedUser value ? value
                : authService.findById(LoginInterceptor.currentUserId(request.getSession(false)))
                        .map(AuthenticatedUser::from).orElse(null);
        model.addAttribute("currentUser", user);
        model.addAttribute("isLoggedIn", user != null);
        int quantity = user == null ? 0 : cart.countTotalQuantity(cart.getCartItems(request.getSession(false)));
        model.addAttribute("cartQuantity", quantity);
        request.setAttribute("uiIdentity", user);
        request.setAttribute("uiCartQuantity", quantity);
        model.addAttribute("isAdmin", user != null && user.isAdmin());
        model.addAttribute("showDemoAccounts", environment.acceptsProfiles(Profiles.of("b-demo")));
    }
    /** Exception rendering uses the already checked identity without another database query. */
    public void restore(HttpServletRequest request, Model model) {
        Object checked = request.getAttribute("uiIdentity");
        if (!(checked instanceof AuthenticatedUser)) checked = request.getAttribute(LoginInterceptor.CURRENT_USER);
        AuthenticatedUser user = checked instanceof AuthenticatedUser value ? value : null;
        model.addAttribute("currentUser", user);
        model.addAttribute("isLoggedIn", user != null);
        model.addAttribute("isAdmin", user != null && user.isAdmin());
        Object quantity = request.getAttribute("uiCartQuantity");
        model.addAttribute("cartQuantity", user != null && quantity instanceof Integer ? quantity : 0);
    }

}
