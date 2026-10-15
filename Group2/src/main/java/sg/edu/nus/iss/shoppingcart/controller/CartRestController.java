package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sg.edu.nus.iss.shoppingcart.service.CartService;
import java.util.Map;
import java.math.BigDecimal;
import java.util.List;
import sg.edu.nus.iss.shoppingcart.dto.CartLine;
import sg.edu.nus.iss.shoppingcart.dto.OrderPricing;

/** Quick add stays on the product page; identity and prices are always server controlled. */
@RestController
public class CartRestController {
    private final CartService cart;
    public CartRestController(CartService cart) { this.cart = cart; }
    public record AddItem(@NotNull @Min(1) Long productId,
                          @NotNull @Min(1) @Max(Integer.MAX_VALUE) @Digits(integer = 10, fraction = 0) BigDecimal quantity,
                          @NotBlank String cartFormToken) { }
    public record SetQuantity(@NotNull @Min(1) @Max(Integer.MAX_VALUE) @Digits(integer = 10, fraction = 0) BigDecimal quantity,
                              @NotBlank String cartFormToken) { }
    public record RemoveItem(@NotBlank String cartFormToken) { }
    public record CartState(int itemCount, int totalQuantity, Map<Long, Integer> quantities,
                            List<CartLine> items, OrderPricing pricing, boolean canCheckout) { }

    @PutMapping("/api/cart/items/{productId}")
    public ResponseEntity<?> update(@PathVariable @Min(1) Long productId,
                                    @Valid @RequestBody SetQuantity form, HttpSession session) {
        synchronized (session) {
            cart.validateFormToken(session, form.cartFormToken());
            cart.updateQuantity(session, productId, form.quantity().intValueExact());
            return state(session);
        }
    }

    private ResponseEntity<?> state(HttpSession session) {
        var items = cart.getCartItems(session);
        var pricing = OrderPricing.calculate(items.stream().map(CartLine::getOriginalSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add), cart.calculateTotal(items));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new CartState(items.size(),
                cart.countTotalQuantity(items), items.stream().collect(
                        java.util.stream.Collectors.toMap(sg.edu.nus.iss.shoppingcart.dto.CartLine::getProductId,
                                sg.edu.nus.iss.shoppingcart.dto.CartLine::getQuantity)), items, pricing, cart.canCheckout(items)));
    }

    @DeleteMapping("/api/cart/items/{productId}")
    public ResponseEntity<?> remove(@PathVariable @Min(1) Long productId,
                                    @Valid @RequestBody RemoveItem form, HttpSession session) {
        synchronized (session) {
            cart.validateFormToken(session, form.cartFormToken());
            cart.removeItem(session, productId);
            return state(session);
        }
    }

    @PostMapping("/api/cart/items")
    public ResponseEntity<?> add(@Valid @RequestBody AddItem form, HttpSession session) {
        synchronized (session) {
            cart.validateFormToken(session, form.cartFormToken());
            cart.addItem(session, form.productId(), form.quantity().intValueExact());
            return state(session);
        }
    }
}
