package sg.edu.nus.iss.shoppingcart.service;

import sg.edu.nus.iss.shoppingcart.dto.CartLine;
import java.util.Map;
import jakarta.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;

/** Business operations, following the CourseService/Impl tutorial. */
public interface CartService {
    String CART_ATTRIBUTE = "cart";
    String CHECKOUT_STATE_ATTRIBUTE = "checkoutState";
    String FORM_TOKEN_ATTRIBUTE = "cartFormToken";

    Long requireUserId(HttpSession session);

    List<CartLine> getCartItems(HttpSession session);

    long countItems(HttpSession session);

    Map<Long, Integer> quantities(HttpSession session);

    long revision(HttpSession session);

    void addItem(HttpSession session, Long productId, int quantity);

    void updateQuantity(HttpSession session, Long productId, int quantity);

    void removeItem(HttpSession session, Long productId);

    void clearCart(HttpSession session);

    Map<Long, Integer> readForCheckout(HttpSession session);

    BigDecimal calculateTotal(List<CartLine> items);

    int countTotalQuantity(List<CartLine> items);

    boolean canCheckout(List<CartLine> items);

    String formToken(HttpSession session);

    void validateFormToken(HttpSession session, String supplied);
}
