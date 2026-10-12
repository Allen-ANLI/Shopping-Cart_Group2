package sg.edu.nus.iss.shoppingcart.dto;

import java.math.BigDecimal;

/** Sales totals for one product, used by the administrator dashboard. */
public record AdminProductSalesDto(Long productId, String name, String nameZh,
                                   BigDecimal unitPrice, long quantity, BigDecimal revenue) {
}
