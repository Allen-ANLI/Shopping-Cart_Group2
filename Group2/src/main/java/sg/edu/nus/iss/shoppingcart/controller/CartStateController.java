package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.service.CartService;

import java.util.Map;

/** Quick cart controls share the existing authenticated session and form token. */
@RestController
public class CartStateController {
    private final CartService cart;
    public CartStateController(CartService cart) { this.cart = cart; }

    @GetMapping("/api/cart/state")
    public ResponseEntity<?> state(HttpSession session) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.VARY, "Cookie")
                .body(cart.snapshot(session));
    }

    @PostMapping("/api/cart/adjust")
    public ResponseEntity<?> adjust(HttpSession session, @RequestParam Long productId, @RequestParam int delta,
                                    @RequestParam(required = false) String cartFormToken) {
        try {
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header(HttpHeaders.VARY, "Cookie")
                    .body(cart.adjustItem(session, productId, delta, cartFormToken));
        } catch (BusinessException ex) {
            return ResponseEntity.badRequest().cacheControl(CacheControl.noStore())
                    .body(Map.of("message", ex.getUserMessage()));
        }
    }
}
