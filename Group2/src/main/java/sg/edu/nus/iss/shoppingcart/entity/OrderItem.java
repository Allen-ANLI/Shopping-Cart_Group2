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

/**
 * 订单明细：保存商品的成交名称、单价和购买数量。
 *
 * @author 邱弈杰
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(
            name = "product_name_snapshot",
            nullable = false,
            length = 100
    )
    private String productNameSnapshot;

    @Column(
            name = "unit_price",
            nullable = false,
            precision = 12,
            scale = 2
    )
    private BigDecimal unitPrice;

    @Column(precision=12, scale=2)
    private BigDecimal originalUnitPrice;
    public BigDecimal getOriginalUnitPrice() { return originalUnitPrice == null ? unitPrice : originalUnitPrice; }

    @Column(nullable = false)
    private int quantity;

    // JPA 读取数据库记录时需要。
    protected OrderItem() {
    }

    // 结账时创建一条订单明细。
    public OrderItem(Order order, Product product, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("购买数量必须大于 0");
        }

        this.order = order;
        this.product = product;

        // 保存成交时的名称与单价，供历史订单使用。
        this.productNameSnapshot = product.getName();
        this.unitPrice = product.getEffectivePrice();
        this.originalUnitPrice = product.getPrice();

        this.quantity = quantity;
    }

    public Long getId() {
        return id;
    }

    public Order getOrder() {
        return order;
    }

    public Product getProduct() {
        return product;
    }

    public String getProductNameSnapshot() {
        return productNameSnapshot;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public int getQuantity() {
        return quantity;
    }

    // 明细小计 = 成交单价 × 数量。
    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}