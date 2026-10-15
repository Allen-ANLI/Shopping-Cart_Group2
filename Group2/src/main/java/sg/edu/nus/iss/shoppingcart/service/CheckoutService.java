package sg.edu.nus.iss.shoppingcart.service;

import sg.edu.nus.iss.shoppingcart.entity.Order;
import java.util.Map;

/** Business operations, following the CourseService/Impl tutorial. */
public interface CheckoutService {
    Order placeOrder(Long userId, Map<Long, Integer> cart, String checkoutToken, Long addressId);

    Order checkout(Long userId, Map<Long, Integer> cart,
                          String checkoutToken);

    Order checkout(Long userId, Map<Long, Integer> cart,
                          String checkoutToken, Long addressId);

    Order checkout(Long userId, Map<Long, Integer> cart, String checkoutToken, Long addressId,
                          PaymentService.Request payment);
}
