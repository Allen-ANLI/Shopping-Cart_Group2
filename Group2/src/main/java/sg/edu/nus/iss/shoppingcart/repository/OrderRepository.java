package sg.edu.nus.iss.shoppingcart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sg.edu.nus.iss.shoppingcart.entity.Order;

import java.util.Optional;

/**
 * 订单的数据库访问接口。
 *
 * @author 邱弈杰
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    // 根据结账请求标识和用户 ID，查找这个用户已提交的订单。
    Optional<Order> findByCheckoutTokenAndUser_Id(
            String checkoutToken,
            Long userId
    );
}