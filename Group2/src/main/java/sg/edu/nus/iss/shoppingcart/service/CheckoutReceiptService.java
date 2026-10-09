package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.dto.CheckoutReceipt;
import sg.edu.nus.iss.shoppingcart.entity.Order;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.repository.OrderRepository;
import sg.edu.nus.iss.shoppingcart.repository.OrderItemRepository;

/**
 * 查询当前用户本次订单的成交数据，供成功页面显示。
 * @author 邱弈杰
 */
@Service
public class CheckoutReceiptService {
    private final OrderRepository orders;
    private final OrderItemRepository items;

    public CheckoutReceiptService(OrderRepository orders, OrderItemRepository items) {
        this.orders = orders;
        this.items = items;
    }

    @Transactional(readOnly = true)
    public CheckoutReceipt forUser(String token, Long userId) {
        Order order = orders.findByCheckoutTokenAndUser_Id(token, userId)
                .orElseThrow(() -> new BusinessException("Order not found"));
        var lines = items.findByOrder_IdOrderByIdAsc(order.getId()).stream()
                .map(item -> new CheckoutReceipt.Line(item.getProductNameSnapshot(),
                        item.getUnitPrice(), item.getQuantity(), item.getSubtotal())).toList();
        return new CheckoutReceipt(order.getId(), order.getCreatedAt(), order.getTotalAmount(), lines);
    }
}
