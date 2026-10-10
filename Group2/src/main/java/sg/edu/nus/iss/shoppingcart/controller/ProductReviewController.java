package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.exception.NotAuthenticatedException;
import sg.edu.nus.iss.shoppingcart.interceptor.CurrentUser;
import sg.edu.nus.iss.shoppingcart.service.CartService;
import sg.edu.nus.iss.shoppingcart.service.ProductReviewService;
import java.util.Map;

@RestController
@RequestMapping("/api/products/{id}/reviews")
public class ProductReviewController {
    private final ProductReviewService reviews;
    private final CartService cart;

    public ProductReviewController(ProductReviewService reviews, CartService cart) {
        this.reviews = reviews; this.cart = cart;
    }

    @GetMapping
    public ResponseEntity<ProductReviewService.ReviewSummary> read(@PathVariable Long id, HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
                .body(reviews.read(id, CurrentUser.getId(request.getSession(false))));
    }

    @PostMapping
    public ResponseEntity<?> save(@PathVariable Long id, @Valid @RequestBody ReviewRequest body,
                                  HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Long userId = CurrentUser.getId(session);
        if (userId == null) throw new NotAuthenticatedException();
        synchronized (session) {
            try {
                cart.validateFormToken(session, body.cartFormToken());
            } catch (BusinessException ex) {
                return ResponseEntity.status(403).body(Map.of("status", 403, "message",
                        sg.edu.nus.iss.shoppingcart.service.UiText.localize("This form has expired. Refresh the page and try again."),
                        "code", "form.expired"));
            }
            return ResponseEntity.ok(reviews.save(id, userId, body.rating(), body.comment()));
        }
    }

    public record ReviewRequest(@NotNull @Min(1) @Max(5) Integer rating,
                                @NotBlank @Size(min = 5, max = 1000) String comment,
                                String cartFormToken) {}
}
