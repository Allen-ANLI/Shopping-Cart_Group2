package sg.edu.nus.iss.shoppingcart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单历史列表的一行数据。
 *
 * <p><b>为什么需要这个 DTO：</b>历史列表要显示每张订单的"件数"，
 * 但 {@code Order.items} 是懒加载集合，直接在模板里写
 * {@code order.items.size()} 会在 {@code open-in-view=false} 下抛
 * LazyInitializationException。而给分页查询加 {@code join fetch o.items}
 * 也不行——Hibernate 在 join fetch 集合的同时分页会退化成
 * "applying in memory"，页码失效，数据一多就全表加载。</p>
 *
 * <p>所以走投影查询：一条 SQL 直接把订单主表字段和件数一起查出来，
 * 映射到这个不可变的记录类型。既能分页，也不会触发懒加载。</p>
 *
 * @author 蔡千一（Module E）
 */
public class OrderSummaryDto {

    /** 订单 ID，用于拼接详情页链接。 */
    private final Long id;

    /** 下单时间，历史列表按它倒序。 */
    private final LocalDateTime createdAt;

    /** 订单总金额。 */
    private final BigDecimal totalAmount;

    /** 商品件数，由 count 子查询得出。 */
    private final long itemCount;

    /**
     * 供 JPQL 投影表达式使用的构造方法。
     *
     * <p>参数顺序必须和Repository 里 {@code select} 的顺序一致，
     * 否则 Spring Data 会在启动时报构造参数不匹配。</p>
     *
     * @param id订单 ID
     * @param createdAt 下单时间
     * @param totalAmount 总金额
     * @param itemCount 件数
     */
    public OrderSummaryDto(Long id, LocalDateTime createdAt, BigDecimal totalAmount, long itemCount) {
        this.id = id;
        this.createdAt = createdAt;
        this.totalAmount = totalAmount;
        this.itemCount = itemCount;
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

    public long getItemCount() {
        return itemCount;
    }
}