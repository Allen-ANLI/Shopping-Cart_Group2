package sg.edu.nus.iss.shoppingcart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单：记录购买者、成交时间和总金额。
 *
 * @author 邱弈杰
 */
@Entity
@Table(name = "orders")
public class Order {

    @jakarta.persistence.Embedded
    private ShippingSnapshot shipping;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(
            name = "total_amount",
            nullable = false,
            precision = 14,
            scale = 2
    )
    private BigDecimal totalAmount;

    @Column(
            name = "checkout_token",
            nullable = false,
            unique = true,
            length = 36
    )
    private String checkoutToken;

    // Null denotes an order completed before simulated payments were introduced.
    @Column(name = "payment_status", length = 20)
    private String paymentStatus;
    @Column(length = 30)
    private String paymentMethod;
    @Column(length = 50)
    private String paymentReference;
    private LocalDateTime paidAt;

    @Column(length = 30)
    private String paymentInstrument;
    @Column(length = 8)
    private String paymentLastDigits;
    @Column(length = 15)
    private String cryptoAsset;
    @Column(length = 20)
    private String cryptoNetwork;
    @Column(precision = 22, scale = 8)
    private BigDecimal cryptoAmount;
    private LocalDateTime shipmentDeliveredAt;
    private LocalDateTime receiptConfirmedAt;

    @Column(precision=14, scale=2) private BigDecimal originalSubtotal;
    @Column(precision=14, scale=2) private BigDecimal discountAmount;
    @Column(precision=14, scale=2) private BigDecimal merchandiseSubtotal;
    @Column(precision=14, scale=2) private BigDecimal gstAmount;
    public BigDecimal getOriginalSubtotal() { return originalSubtotal == null ? totalAmount : originalSubtotal; }
    public BigDecimal getDiscountAmount() { return discountAmount == null ? BigDecimal.ZERO : discountAmount; }
    public BigDecimal getMerchandiseSubtotal() { return merchandiseSubtotal == null ? totalAmount : merchandiseSubtotal; }
    public BigDecimal getGstAmount() { return gstAmount == null ? BigDecimal.ZERO : gstAmount; }
    public boolean isPendingPayment() { return "PENDING".equals(paymentStatus); }
    public boolean isPaid() { return paymentStatus == null || "PAID".equals(paymentStatus); }
    public void awaitPayment(sg.edu.nus.iss.shoppingcart.dto.OrderPricing pricing) {
        originalSubtotal=pricing.originalSubtotal(); discountAmount=pricing.discountAmount();
        merchandiseSubtotal=pricing.subtotal(); gstAmount=pricing.gstAmount(); totalAmount=pricing.total();
        paymentStatus="PENDING";
    }
    public String getPaymentInstrument() { return paymentInstrument; }
    public String getPaymentLastDigits() { return paymentLastDigits; }
    public String getCryptoAsset() { return cryptoAsset; }
    public String getCryptoNetwork() { return cryptoNetwork; }
    public BigDecimal getCryptoAmount() { return cryptoAmount; }
    public LocalDateTime getShipmentDeliveredAt() { return shipmentDeliveredAt; }
    public LocalDateTime getReceiptConfirmedAt() { return receiptConfirmedAt; }
    public boolean isReceiptConfirmed() { return receiptConfirmedAt != null; }
    public void recordPaymentInstrument(String instrument, String lastDigits, String asset, String network, BigDecimal amount) {
        paymentInstrument = instrument; paymentLastDigits = lastDigits;
        cryptoAsset = asset; cryptoNetwork = network; cryptoAmount = amount;
    }
    public void confirmReceipt() {
        if (!("PAID".equals(paymentStatus) || paymentStatus == null)) {
            throw new IllegalStateException("An unpaid order cannot be confirmed");
        }
        if (shipmentDeliveredAt == null) shipmentDeliveredAt = paidAt != null ? paidAt : createdAt;
        if (receiptConfirmedAt == null) receiptConfirmedAt = LocalDateTime.now();
    }

    public String getPaymentStatus() { return paymentStatus == null ? "LEGACY_COMPLETED" : paymentStatus; }
    public String getPaymentMethod() { return paymentMethod; }
    public String getPaymentReference() { return paymentReference; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public void recordPayment(String method, String reference) {
        paymentStatus = "PAID"; paymentMethod = method; paymentReference = reference;
        paidAt = LocalDateTime.now();
        // Simulation fast-forwards through every shipment stage immediately.
        shipmentDeliveredAt = paidAt;
    }

    // JPA 从数据库读取订单、创建对象时需要无参数构造方法。
    protected Order() {
    }

    // 下单时由业务代码调用这个构造方法。
    public Order(User user, BigDecimal totalAmount, String checkoutToken) {
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.totalAmount = totalAmount;
        this.checkoutToken = checkoutToken;
    }

    public Order(User user, BigDecimal totalAmount, String checkoutToken, ShippingSnapshot shipping) {
        this(user, totalAmount, checkoutToken);
        this.shipping = shipping;
    }

    public ShippingSnapshot getShipping() { return shipping; }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public String getCheckoutToken() {
        return checkoutToken;
    }
}
