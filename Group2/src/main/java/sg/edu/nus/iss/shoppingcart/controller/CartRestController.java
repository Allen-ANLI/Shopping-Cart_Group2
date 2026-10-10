package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sg.edu.nus.iss.shoppingcart.service.CartService;
import java.util.Map;

/** Quick add stays on the product page; identity and prices are always server controlled. */
@RestController
public class CartRestController {
    private final CartService cart;
    public CartRestController(CartService cart) { this.cart = cart; }
    public record AddItem(@NotNull @Min(1) Long productId,
                          @Min(1) @Max(99) int quantity, @NotBlank String cartFormToken) { }

    @PostMapping("/api/cart/items")
    public ResponseEntity<?> add(@Valid @RequestBody AddItem form, HttpSession session) {
        synchronized (session) {
            cart.validateFormToken(session, form.cartFormToken());
            cart.addItem(session, form.productId(), form.quantity());
            var items = cart.getCartItems(session);
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(
                    Map.of("itemCount", items.size(), "totalQuantity", cart.countTotalQuantity(items)));
        }
    }
}
