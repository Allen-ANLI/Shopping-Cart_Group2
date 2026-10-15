package sg.edu.nus.iss.shoppingcart.service;

import sg.edu.nus.iss.shoppingcart.dto.AdminProductSalesDto;
import java.math.BigDecimal;
import java.util.List;

/** Administrator sales operations following the classroom service interface pattern. */
public interface AdminDashboardService {
    Dashboard read();

    record Dashboard(BigDecimal totalSales, long orderCount, long itemCount,
                     List<AdminProductSalesDto> productSales) { }
}
