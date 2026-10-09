package sg.edu.nus.iss.shoppingcart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sg.edu.nus.iss.shoppingcart.entity.OrderItem;

import java.util.List;

/**
 * 订单明细读取接口 —— E 专用。
 *
 * <p><b>为什么不挂到 {@code Order.items}：</b>
 * D 维护的 {@code Order} 实体刻意没有明细集合，这是他的领域选择，
 * E 不应该改。所以 E 用独立查询把明细取出来，在 Service 层组装成
 * 详情视图对象。这样 E 和 D 的实体可以各自演进。</p>
 *
 * @author 蔡千一（Module E）
 */
public interface OrderItemQueryRepository extends JpaRepository<OrderItem, Long> {

    /**
     * 按订单 ID 查全部明细。
     *
     * <p>排序 {@code id ASC} 保证明细按加入购物车的顺序显示——
     * 订单的打印和小计需要用户看到的顺序稳定可预期。</p>
     *
     * @param orderId 订单 ID
     * @return 明细列表；无明细时返回空列表而非 null
     */
    List<OrderItem> findByOrder_IdOrderByIdAsc(Long orderId);

    /**
     * 统计一张订单的商品总件数。
     *
     * <p>历史列表要显示"共 N 件"，走 SQL 聚合而不是读出明细再数。</p>
     *
     * @param orderId 订单 ID
     * @return 件数总和
     */
    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM OrderItem i WHERE i.order.id = :orderId")
    long sumQuantityByOrderId(@Param("orderId") Long orderId);
}