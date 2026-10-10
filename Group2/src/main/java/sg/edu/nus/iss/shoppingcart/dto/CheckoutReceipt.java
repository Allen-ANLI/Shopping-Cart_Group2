package sg.edu.nus.iss.shoppingcart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 结账成功页使用的订单成交快照。
 * @author 邱弈杰
 */
public record CheckoutReceipt(Long id, LocalDateTime createdAt, BigDecimal totalAmount,
                              List<Line> items, sg.edu.nus.iss.shoppingcart.entity.ShippingSnapshot shipping, PaymentDetails payment) {
    public CheckoutReceipt(Long id, LocalDateTime createdAt, BigDecimal totalAmount, List<Line> items,
                           sg.edu.nus.iss.shoppingcart.entity.ShippingSnapshot shipping) {
        this(id, createdAt, totalAmount, items, shipping, null);
    }
    public CheckoutReceipt(Long id, LocalDateTime createdAt, BigDecimal totalAmount, List<Line> items) {
        this(id, createdAt, totalAmount, items, null);
    }
    /**
     * 订单商品的成交名称、单价、数量与小计。
     * @author 邱弈杰
     */
    public record Line(String productName, BigDecimal unitPrice, int quantity,
                       BigDecimal subtotal) {}
}
