package sg.edu.nus.iss.shoppingcart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情视图数据。
 *
 * <p>把订单主表和明细列表打包成一个对象传给模板，
 * 避免在 Thymeleaf 里做数据拼装。</p>
 *
 * <p><b>关键点：</b>{@link OrderItemLine} 里的名称和单价来自明细快照，
 * 不是来自商品表。所以管理员在后台改了商品名或价格，
 * 这里显示的仍然是成交时的值——这是"历史订单不被商品修改破坏"的
 * 直接体现。</p>
 *
 * @author 蔡千一（Module E）
 */
public class OrderDetailDto {

    /** 订单 ID。 */
    private final Long id;

    /** 下单时间。 */
    private final LocalDateTime createdAt;

    /** 订单总金额。 */
    private final BigDecimal totalAmount;

    /** 明细行。 */
    private final List<OrderItemLine> items;
    private sg.edu.nus.iss.shoppingcart.entity.ShippingSnapshot shipping;
    private PaymentDetails payment;
    private OrderPricing pricing;
    public OrderPricing getPricing() { return pricing; }
    public OrderDetailDto withPricing(sg.edu.nus.iss.shoppingcart.entity.Order order) {
        pricing = new OrderPricing(order.getOriginalSubtotal(), order.getDiscountAmount(), order.getMerchandiseSubtotal(), order.getGstAmount(), order.getTotalAmount()); return this;
    }
    public boolean isPendingPayment() { return payment != null && "PENDING".equals(payment.status()); }

    private LocalDateTime shipmentDeliveredAt;
    private LocalDateTime receiptConfirmedAt;
    public LocalDateTime getShipmentDeliveredAt() { return shipmentDeliveredAt; }
    public LocalDateTime getReceiptConfirmedAt() { return receiptConfirmedAt; }
    public boolean isReceiptConfirmed() { return receiptConfirmedAt != null; }
    public OrderDetailDto withShipment(LocalDateTime delivered, LocalDateTime confirmed) {
        shipmentDeliveredAt = delivered; receiptConfirmedAt = confirmed; return this;
    }
    public PaymentDetails getPayment() { return payment; }
    public OrderDetailDto withPayment(PaymentDetails payment) { this.payment = payment; return this; }

    public OrderDetailDto(Long id, LocalDateTime createdAt, BigDecimal totalAmount, List<OrderItemLine> items,
                          sg.edu.nus.iss.shoppingcart.entity.ShippingSnapshot shipping) {
        this(id, createdAt, totalAmount, items);
        this.shipping = shipping;
    }

    public sg.edu.nus.iss.shoppingcart.entity.ShippingSnapshot getShipping() { return shipping; }

    /**
     * 构造详情对象。
     *
     * @param id           订单 ID
     * @param createdAt    下单时间
     * @param totalAmount  总金额
     * @param items        明细行
     */
    public OrderDetailDto(Long id, LocalDateTime createdAt,
                          BigDecimal totalAmount, List<OrderItemLine> items) {
        this.id = id;
        this.createdAt = createdAt;
        this.totalAmount = totalAmount;
        this.items = items;
    }

    /** 商品总件数。 */
    public int getTotalQuantity() {
        return items.stream().mapToInt(OrderItemLine::getQuantity).sum();
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public List<OrderItemLine> getItems() {
        return items;
    }

    /**
     * 订单明细的一行。
     *
     * @author 蔡千一（Module E）
     */
    public static class OrderItemLine {
        private BigDecimal originalUnitPrice;
        public BigDecimal getOriginalUnitPrice() { return originalUnitPrice == null ? unitPrice : originalUnitPrice; }
        public OrderItemLine withOriginalPrice(BigDecimal price) { originalUnitPrice=price; return this; }
        private Long productId;
        public Long getProductId() { return productId; }
        public OrderItemLine withProductId(Long id) { productId = id; return this; }


        /** 成交时的商品名称快照。 */
        private final String productName;

        /** 成交时的单价快照。 */
        private final BigDecimal unitPrice;

        /** 购买数量。 */
        private final int quantity;

        /**
         * 构造明细行。
         *
         * @param productName 名称快照
         * @param unitPrice   单价快照
         * @param quantity    数量
         */
        public OrderItemLine(String productName, BigDecimal unitPrice, int quantity) {
            this.productName = productName;
            this.unitPrice = unitPrice;
            this.quantity = quantity;
        }

        /** 小计 = 成交单价 × 数量。 */
        public BigDecimal getSubtotal() {
            return unitPrice.multiply(BigDecimal.valueOf(quantity));
        }

        public String getProductName() {
            return productName;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public int getQuantity() {
            return quantity;
        }
    }
}
