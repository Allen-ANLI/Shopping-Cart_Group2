package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.entity.Order;
import sg.edu.nus.iss.shoppingcart.repository.OrderRepository;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 结账入口：校验请求并处理同一次结账的重复提交。
 * @author 邱弈杰
 */
@Service
public class CheckoutService {

    private final OrderRepository orderRepository;
    private final CheckoutTransactionService transactionService;

    public CheckoutService(OrderRepository orderRepository,
                           CheckoutTransactionService transactionService) {
        this.orderRepository = orderRepository;
        this.transactionService = transactionService;
    }

    // 外层不参与事务，确保内层事务结束后再处理重复请求。
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Order checkout(Long userId, Map<Long, Integer> cart,
                          String checkoutToken) {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("请先登录");
        }
        if (checkoutToken == null || checkoutToken.isBlank()
                || checkoutToken.length() > 36) {
            throw new IllegalArgumentException("结账请求标识不合法");
        }

        Optional<Order> existing = orderRepository
                .findByCheckoutTokenAndUser_Id(checkoutToken, userId);
        if (existing.isPresent()) {
            return existing.get();
        }

        // 使用购物车副本，服务不会修改调用方传入的购物车。
        Map<Long, Integer> cartSnapshot = cart == null
                ? Map.of() : new LinkedHashMap<>(cart);

        try {
            return transactionService.createOrder(
                    userId, cartSnapshot, checkoutToken);
        } catch (DataIntegrityViolationException ex) {
            // 两次请求同时到达时，数据库唯一约束阻止第二张订单。
            // 到这里，失败的写入事务已经回滚，可以重新查询。
            Optional<Order> completed = orderRepository
                    .findByCheckoutTokenAndUser_Id(checkoutToken, userId);
            if (completed.isPresent()) {
                return completed.get();
            }

            // 没有找到本人已完成的订单，说明不能当作重复提交处理。
            throw ex;
        }
    }
}
