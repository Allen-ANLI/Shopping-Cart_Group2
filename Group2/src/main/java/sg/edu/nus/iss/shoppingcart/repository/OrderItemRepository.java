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
}