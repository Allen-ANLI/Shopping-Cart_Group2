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