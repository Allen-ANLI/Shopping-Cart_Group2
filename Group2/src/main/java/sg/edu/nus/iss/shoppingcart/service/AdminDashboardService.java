package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.dto.AdminProductSalesDto;
import sg.edu.nus.iss.shoppingcart.repository.OrderItemQueryRepository;
import sg.edu.nus.iss.shoppingcart.repository.OrderQueryRepository;

import java.math.BigDecimal;
import java.util.List;

/** Read-only sales summary for the administrator dashboard. */
@Service
@Transactional(readOnly = true)
public class AdminDashboardService {
    private final OrderQueryRepository orders;
    private final OrderItemQueryRepository items;

    public AdminDashboardService(OrderQueryRepository orders, OrderItemQueryRepository items) {
        this.orders = orders;
        this.items = items;
    }

    public Dashboard read() {
        List<AdminProductSalesDto> sales = items.findPaidProductSales();
        BigDecimal total = sales.stream().map(AdminProductSalesDto::revenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long quantity = sales.stream().mapToLong(AdminProductSalesDto::quantity).sum();
        return new Dashboard(total, orders.countPaidOrders(), quantity, sales);
    }

    public record Dashboard(BigDecimal totalSales, long orderCount, long itemCount,
                            List<AdminProductSalesDto> productSales) { }
}
