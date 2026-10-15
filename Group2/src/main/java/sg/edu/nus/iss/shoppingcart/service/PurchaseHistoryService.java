package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.data.domain.Page;
import sg.edu.nus.iss.shoppingcart.dto.OrderDetailDto;
import sg.edu.nus.iss.shoppingcart.dto.OrderSummaryDto;
import java.math.BigDecimal;

/** Business operations, following the CourseService/Impl tutorial. */
public interface PurchaseHistoryService {
    int PAGE_SIZE = 5;

    Page<OrderSummaryDto> findHistory(Long userId, int page);

    OrderDetailDto findOrderDetailForUser(Long orderId, Long userId);

    long countOrders(Long userId);

    BigDecimal calculateLifetimeSpend(Long userId);
}
