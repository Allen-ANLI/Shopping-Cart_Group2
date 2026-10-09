package sg.edu.nus.iss.shoppingcart.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 移除购物项的表单。
 * @author Letian Xie
 */
public class CartProductForm {
    @NotNull @Positive
    private Long productId;
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
}
