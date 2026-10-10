package sg.edu.nus.iss.shoppingcart.service;

import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.entity.Order;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 结账流程协调：准备请求标识、保存订单、成功后清空 Session。
 * Session 锁保护同会话并发；数据库幂等与订单事务由 D 的 CheckoutService 保证。
 * C 的版本检查思路由 Letian Xie 提供；此文件作为 D 的结账协调实现交付。
 * @author 邱弈杰
 */
@Service
public class CheckoutCoordinator {
    public static final String SESSION_ATTRIBUTE = CartService.CHECKOUT_STATE_ATTRIBUTE;
    private final CartService cartService;
    private final CheckoutService checkoutService;

    public CheckoutCoordinator(CartService cartService, CheckoutService checkoutService) {
        this.cartService = cartService;
        this.checkoutService = checkoutService;
    }

    private State state(HttpSession session) {
        cartService.revision(session); // 校验身份并清理切换账号时的旧状态。
        Object stored = session.getAttribute(SESSION_ATTRIBUTE);
        if (stored instanceof State state) { return state; }
        State state = new State();
        session.setAttribute(SESSION_ATTRIBUTE, state);
        return state;
    }

    public String prepare(HttpSession session) {
        synchronized (session) {
            State state = state(session);
            long revision = cartService.revision(session);
            if (state.token == null || state.revision != revision) {
                state.token = UUID.randomUUID().toString();
                state.revision = revision;
                session.setAttribute(SESSION_ATTRIBUTE, state);
            }
            return state.token;
        }
    }

    public Long submit(HttpSession session, String token) {
        return submit(session, token, null);
    }

    public Long submit(HttpSession session, String token, Long addressId) {
        synchronized (session) {
            Long userId = cartService.requireUserId(session);
            State state = state(session);
            if (token != null && state.completed.containsKey(token)) {
                return state.completed.get(token);
            }
            if (token == null || !token.equals(state.token)
                    || state.revision != cartService.revision(session)) {
                throw new BusinessException("Your cart changed or this checkout form expired. Review it and try again.");
            }
            // 调用的是 Spring 代理，返回时数据库事务已经完成；异常时下方清空不会执行。
            Order order = checkoutService.checkout(userId, cartService.readForCheckout(session), token, addressId);
            cartService.clearCart(session);
            state.completed.put(token, order.getId());
            while (state.completed.size() > 5) {
                state.completed.remove(state.completed.keySet().iterator().next());
            }
            state.token = null;
            session.setAttribute(SESSION_ATTRIBUTE, state);
            return order.getId();
        }
    }

    public Long completedOrder(HttpSession session, String token) {
        synchronized (session) {
            Long orderId = state(session).completed.get(token);
            if (orderId == null) { throw new BusinessException("This checkout receipt has expired"); }
            return orderId;
        }
    }

    /**
     * 结账请求标识、购物车版本与最近完成回执。
     * @author 邱弈杰
     */
    private static final class State implements Serializable {
        @Serial private static final long serialVersionUID = 1L;
        private String token;
        private long revision;
        private final Map<String, Long> completed = new LinkedHashMap<>();
    }
}
