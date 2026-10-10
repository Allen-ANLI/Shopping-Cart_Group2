package sg.edu.nus.iss.shoppingcart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sg.edu.nus.iss.shoppingcart.entity.OrderItem;

import java.util.List;

/**
 * 订单明细的数据库访问接口。
 *
 * @author 邱弈杰
 */
public interface OrderItemRepository
        extends JpaRepository<OrderItem, Long> {

    // 查询某张订单的全部明细，按明细 ID 升序排列。
    List<OrderItem> findByOrder_IdOrderByIdAsc(Long orderId);

    @org.springframework.data.jpa.repository.Query("select count(i) > 0 from OrderItem i where i.order.user.id = :userId and i.product.id = :productId and (i.order.paymentStatus = 'PAID' or i.order.paymentStatus is null)")
    boolean hasPurchased(@org.springframework.data.repository.query.Param("userId") Long userId,
                         @org.springframework.data.repository.query.Param("productId") Long productId);

    @org.springframework.data.jpa.repository.Query("select count(i) > 0 from OrderItem i where i.order.user.id = :userId and i.product.id = :productId and i.order.receiptConfirmedAt is not null and (i.order.paymentStatus = 'PAID' or i.order.paymentStatus is null)")
    boolean hasConfirmedPurchase(@org.springframework.data.repository.query.Param("userId") Long userId,
                                 @org.springframework.data.repository.query.Param("productId") Long productId);

    @org.springframework.data.jpa.repository.Query("select i from OrderItem i join fetch i.product where i.order.user.id = :userId and (i.order.paymentStatus = 'PAID' or i.order.paymentStatus is null)")
    List<OrderItem> purchasesFor(@org.springframework.data.repository.query.Param("userId") Long userId);
}
