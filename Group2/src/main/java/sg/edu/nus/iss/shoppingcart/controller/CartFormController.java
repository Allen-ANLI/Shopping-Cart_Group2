package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sg.edu.nus.iss.shoppingcart.dto.CartFormState;
import sg.edu.nus.iss.shoppingcart.exception.NotAuthenticatedException;
import sg.edu.nus.iss.shoppingcart.service.CartService;

import java.util.Map;

/**
 * 为 Angular 详情页提供当前登录会话的加购令牌；写操作仍提交普通购物车表单。
 * @author Letian Xie
 */
@RestController
public class CartFormController {
    private final CartService cart;

    public CartFormController(CartService cart) { this.cart = cart; }

    @GetMapping("/api/cart/form")
    public ResponseEntity<?> form(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) { return loginRequired(); }
        synchronized (session) {
            try {
                var items = cart.getCartItems(session);
                return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                        .header(HttpHeaders.VARY, "Cookie")
                        .body(new CartFormState(cart.formToken(session), items.size(),
                                cart.countTotalQuantity(items), items.stream().collect(java.util.stream.Collectors.toMap(
                                        sg.edu.nus.iss.shoppingcart.dto.CartLine::getProductId,
                                        sg.edu.nus.iss.shoppingcart.dto.CartLine::getQuantity))));
            } catch (NotAuthenticatedException | IllegalStateException ex) {
                return loginRequired();
            }
        }
    }

    private ResponseEntity<?> loginRequired() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).cacheControl(CacheControl.noStore())
                .body(Map.of("error", "LOGIN_REQUIRED", "message", "Please log in to continue"));
    }
}
