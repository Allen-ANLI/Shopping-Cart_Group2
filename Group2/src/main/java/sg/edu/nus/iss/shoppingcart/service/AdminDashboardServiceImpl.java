package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.dto.AdminProductSalesDto;
import sg.edu.nus.iss.shoppingcart.repository.OrderItemQueryRepository;
import sg.edu.nus.iss.shoppingcart.repository.OrderQueryRepository;
import java.math.BigDecimal;
import java.util.List;

/** Reads the same persisted paid order lines used for product sales and unit prices. */
@Service
@Transactional(readOnly = true)
public class AdminDashboardServiceImpl implements AdminDashboardService {
    private final OrderQueryRepository orders;
    private final OrderItemQueryRepository items;

    public AdminDashboardServiceImpl(OrderQueryRepository orders, OrderItemQueryRepository items) {
        this.orders = orders;
        this.items = items;
    }

    @Override
    public Dashboard read() {
        List<AdminProductSalesDto> sales = items.findPaidProductSales();
        BigDecimal total = BigDecimal.ZERO;
        long quantity = 0;
        for (AdminProductSalesDto sale : sales) {
            total = total.add(sale.revenue());
            quantity += sale.quantity();
        }
        return new Dashboard(total, orders.countPaidOrders(), quantity, sales);
    }
}
