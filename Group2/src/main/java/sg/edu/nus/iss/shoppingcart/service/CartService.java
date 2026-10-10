package sg.edu.nus.iss.shoppingcart.service;

import sg.edu.nus.iss.shoppingcart.dto.CartLine;
import java.util.Map;
import java.util.LinkedHashMap;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.exception.NotAuthenticatedException;
import sg.edu.nus.iss.shoppingcart.interceptor.CartSessionIdentity;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.model.SessionCart;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 购物车业务。Session 只存 ID 和数量；每次展示重新读取商品价格。
 * 同一 Session 的操作串行执行，避免数量合并和结账清空相互覆盖。
 * @author Letian Xie
 */
@Service
public class CartService {
    public static final String CART_ATTRIBUTE = "cart";
    /** 仅用于身份切换时清理 D 的旧会话状态；C 不实现下单。 */
    public static final String CHECKOUT_STATE_ATTRIBUTE = "checkoutState";
    public static final String FORM_TOKEN_ATTRIBUTE = "cartFormToken";
    public static final int MAX_QUANTITY = 99;
    private final ProductRepository productRepository;

    public CartService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Long requireUserId(HttpSession session) {
        Long userId = CartSessionIdentity.currentUserId(session);
        if (userId == null) {
            reset(session);
            throw new NotAuthenticatedException();
        }
        return userId;
    }

    /** 读取登录身份，账号变更时重新创建购物车。 */
    private SessionCart state(HttpSession session) {
        Long userId = requireUserId(session);
        Object stored = session.getAttribute(CART_ATTRIBUTE);
        if (stored instanceof SessionCart cart && userId.equals(cart.getOwnerUserId())) {
            return cart;
        }
        reset(session);
        SessionCart cart = new SessionCart(userId);
        session.setAttribute(CART_ATTRIBUTE, cart);
        return cart;
    }

    private void reset(HttpSession session) {
        if (session != null) {
            try {
                session.removeAttribute(CART_ATTRIBUTE);
                session.removeAttribute(FORM_TOKEN_ATTRIBUTE);
                session.removeAttribute(CHECKOUT_STATE_ATTRIBUTE);
            } catch (IllegalStateException ignored) {
                // 已销毁的会话没有需要清理的购物车。
            }
        }
    }

    public List<CartLine> getCartItems(HttpSession session) {
        synchronized (session) {
            List<CartLine> lines = new ArrayList<>();
            state(session).getQuantities().entrySet().stream()
                    .sorted(java.util.Map.Entry.comparingByKey())
                    .forEach(entry -> lines.add(new CartLine(entry.getKey(), entry.getValue(),
                            productRepository.findById(entry.getKey()).orElse(null))));
            return List.copyOf(lines);
        }
    }

    public long countItems(HttpSession session) {
        synchronized (session) { return state(session).getQuantities().size(); }
    }

    public Map<Long, Integer> quantities(HttpSession session) {
        synchronized (session) { return state(session).getQuantities(); }
    }

    public long revision(HttpSession session) {
        synchronized (session) { return state(session).getRevision(); }
    }

    public void addItem(HttpSession session, Long productId, int quantity) {
        synchronized (session) {
            validateQuantity(quantity, false);
            SessionCart cart = state(session);
            int combined = cart.getQuantities().getOrDefault(productId, 0) + quantity;
            validateQuantity(combined, false);
            purchasable(productId, combined);
            cart.put(productId, combined);
            session.setAttribute(CART_ATTRIBUTE, cart);
        }
    }

    public void updateQuantity(HttpSession session, Long productId, int quantity) {
        synchronized (session) {
            validateQuantity(quantity, true);
            SessionCart cart = state(session);
            if (!cart.getQuantities().containsKey(productId)) {
                throw new BusinessException("This product is not in your cart");
            }
            if (quantity == 0) {
                cart.remove(productId);
            } else {
                purchasable(productId, quantity);
                cart.put(productId, quantity);
            }
            session.setAttribute(CART_ATTRIBUTE, cart);
        }
    }

    public void removeItem(HttpSession session, Long productId) {
        synchronized (session) {
            SessionCart cart = state(session);
            cart.remove(productId);
            session.setAttribute(CART_ATTRIBUTE, cart);
        }
    }

    /** 订单事务成功返回后调用，避免回滚时丢失购物车。 */
    public void clearCart(HttpSession session) {
        synchronized (session) {
            SessionCart cart = state(session);
            cart.clear();
            session.setAttribute(CART_ATTRIBUTE, cart);
        }
    }

    /** 只传商品 ID 和数量；D 在事务中重新读取价格。 */
    public Map<Long, Integer> readForCheckout(HttpSession session) {
        synchronized (session) {
            Map<Long, Integer> quantities = state(session).getQuantities();
            if (quantities.isEmpty()) {
                throw new BusinessException("Your shopping cart is empty");
            }
            Map<Long, Integer> result = new LinkedHashMap<>();
            quantities.entrySet().stream().sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> {
                        validateQuantity(entry.getValue(), false);
                        purchasable(entry.getKey(), entry.getValue());
                        result.put(entry.getKey(), entry.getValue());
                    });
            return result;
        }
    }

    private Product purchasable(Long productId, int quantity) {
        if (productId == null || productId <= 0) {
            throw new BusinessException("Please select a valid product");
        }
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new BusinessException("This product is no longer available"));
        if (quantity > product.getStockQuantity()) throw new BusinessException("Not enough stock. Please reduce the quantity.");
        return product;
    }

    private void validateQuantity(int quantity, boolean allowZero) {
        if (quantity < (allowZero ? 0 : 1) || quantity > MAX_QUANTITY) {
            throw new BusinessException(allowZero
                    ? "Quantity must be between 0 and 99; use 0 to remove an item"
                    : "Quantity must be between 1 and 99");
        }
    }

    public BigDecimal calculateTotal(List<CartLine> items) {
        return items.stream().map(CartLine::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public int countTotalQuantity(List<CartLine> items) {
        return items.stream().mapToInt(CartLine::getQuantity).sum();
    }

    public boolean canCheckout(List<CartLine> items) {
        return !items.isEmpty() && items.stream().allMatch(CartLine::isAvailable);
    }

    /** 购物车写表单使用的会话令牌。 */
    public String formToken(HttpSession session) {
        synchronized (session) {
            state(session);
            String token = (String) session.getAttribute(FORM_TOKEN_ATTRIBUTE);
            if (token == null) {
                token = UUID.randomUUID().toString();
                session.setAttribute(FORM_TOKEN_ATTRIBUTE, token);
            }
            return token;
        }
    }

    public void validateFormToken(HttpSession session, String supplied) {
        synchronized (session) {
            if (supplied == null || !MessageDigest.isEqual(
                    formToken(session).getBytes(StandardCharsets.UTF_8),
                    supplied.getBytes(StandardCharsets.UTF_8))) {
                throw new BusinessException("This form has expired. Refresh the page and try again.");
            }
        }
    }
}
