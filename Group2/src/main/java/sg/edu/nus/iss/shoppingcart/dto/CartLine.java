package sg.edu.nus.iss.shoppingcart.dto;

import sg.edu.nus.iss.shoppingcart.entity.Product;
import java.math.BigDecimal;

/**
 * 一次请求的购物车展示快照；删除或下架的商品仍能从购物车移除。
 * 本对象不会存入 Session。
 * @author Letian Xie
 */
public final class CartLine {
    private final Long productId;
    private final int quantity;
    private final String productName;
    private final BigDecimal unitPrice;
    private final BigDecimal originalUnitPrice;
    public BigDecimal getOriginalUnitPrice() { return originalUnitPrice; }
    public BigDecimal getOriginalSubtotal() { return originalUnitPrice.multiply(BigDecimal.valueOf(quantity)); }
    private final String problem;

    public CartLine(Long productId, int quantity, Product product) {
        this.productId = productId;
        this.quantity = quantity;
        boolean chinese = org.springframework.context.i18n.LocaleContextHolder.getLocale().getLanguage().equals("zh");
        this.productName = product == null ? (chinese ? "失效商品 #" : "Unavailable product #") + productId
                : chinese && product.getNameZh() != null && !product.getNameZh().isBlank() ? product.getNameZh() : product.getName();
        this.originalUnitPrice = product == null ? BigDecimal.ZERO : product.getPrice();
        this.unitPrice = product == null ? BigDecimal.ZERO : product.getEffectivePrice();
        this.problem = product == null ? "This product has been removed. Please remove it from your cart."
                : !product.isActive() ? "This product is no longer for sale. Please remove it."
                : quantity > product.getStockQuantity() ? "Not enough stock. Please reduce the quantity."
                    : null;
    }

    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getSubtotal() { return unitPrice.multiply(BigDecimal.valueOf(quantity)); }
    public String getProblem() { return problem == null ? null : sg.edu.nus.iss.shoppingcart.service.UiText.localize(problem); }
    public boolean isAvailable() { return problem == null; }
}
