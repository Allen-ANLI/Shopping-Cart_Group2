package sg.edu.nus.iss.shoppingcart.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sg.edu.nus.iss.shoppingcart.dto.OrderSummaryDto;
import sg.edu.nus.iss.shoppingcart.entity.Order;

import java.util.Optional;

/**
 * 订单读取接口 —— E 专用。
 *
 * <p><b>与 D 的 {@code OrderRepository} 的分工：</b>
 * D 维护写入路径（下单、核价、事务保存、防重复下单），
 * 这个接口只服务 E 的读取路径（历史列表、订单详情、后台统计）。
 * 两者可以共存，合并时不会冲突。</p>
 *
 * @author 蔡千一（Module E）
 * @author OpenAI Codex (stable pagination review)
 */
public interface OrderQueryRepository extends JpaRepository<Order, Long> {

    /**
     * 分页查询某个用户自己的订单摘要（含件数），按下单时间倒序。
     *
     * <p><b>核心查询。</b>用标量子查询算件数，避开两个坑：</p>
     * <ul>
     *   <li>{@code user.id = :userId} 写进 where，越权数据不会进入结果；</li>
     *   <li>件数用 {@code (SELECT SUM(i.quantity) FROM OrderItem i WHERE i.order.id = o.id)}
     *       而不是 {@code left join o.items}。因为 D 维护的 {@code Order}
     *       刻意没有明细集合，JPQL 里写 {@code o.items} 会直接报
     *       "Could not resolve attribute 'items'"。</li>
     * </ul>
     *
     * <p><b>为什么不用 {@code join fetch}：</b>Hibernate 在 join fetch 集合的
     * 同时分页会退化成 "applying in memory"，页码失效。标量子查询不碰集合，
     * 分页正常下推到 SQL。</p>
     *
     * <p>{@code countQuery} 必须单独写：带子查询和别名的 JPQL 无法直接用作
     * count 查询，Spring Data 会解析失败。</p>
     *
     * @param userId   当前登录用户 ID
     * @param pageable 分页参数
     * @return 分页订单摘要
     */
    @Query(value = "SELECT new sg.edu.nus.iss.shoppingcart.dto.OrderSummaryDto("
            + "o.id, o.createdAt, o.totalAmount, "
            + "(SELECT COALESCE(SUM(i.quantity), 0) FROM OrderItem i WHERE i.order.id = o.id)) "
            + "FROM Order o "
            + "WHERE o.user.id = :userId "
            + "ORDER BY o.createdAt DESC, o.id DESC",
            countQuery = "SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId")
    Page<OrderSummaryDto> findOrderSummariesByUserId(@Param("userId") Long userId,
                                                     Pageable pageable);

    /**
     * 按订单 ID 查询，<b>同时校验归属</b>。
     *
     * <p>这是防越权的关键查询：{@code o.user.id = :userId} 下推到 SQL，
     * 别人的订单直接查不出来。换个订单号访问别人的订单，
     * 结果就是空，Service 据此返回统一的"找不到"提示。</p>
     *
     * @param orderId 订单 ID
     * @param userId  当前登录用户 ID
     * @return 订单；不存在或不属于该用户时为空
     */
    Optional<Order> findByIdAndUserId(@Param("orderId") Long orderId,
@Param("userId") Long userId);

    /**
     * 统计某用户的历史订单总数。
     *
     * <p>用数据库 {@code count} 而不是把订单全部读进内存再数。
     * 历史页顶部要显示"共N 笔订单"，订单多时这个差别很明显。</p>
     *
     * @param userId 用户 ID
     * @return 订单数
     */
    long countByUser_Id(Long userId);

    /**
     * 统计某用户的历史订单总金额（不含已取消的订单）。
     *
     * <p>同样下推到 SQL 做 {@code sum}，不在Java 层累加。</p>
     *
     * @param userId 用户 ID
     * @return 累计金额；没有订单时返回 0
     */
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o "
            + "WHERE o.user.id = :userId")
    java.math.BigDecimal sumTotalAmountByUserId(@Param("userId") Long userId);
}
