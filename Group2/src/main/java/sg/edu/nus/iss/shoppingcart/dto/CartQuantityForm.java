package sg.edu.nus.iss.shoppingcart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * C 购物车表单。数量必须为正整数，服务层按实际库存校验上限。
 * @author Letian Xie
 */
public class CartQuantityForm {
    @NotNull @Positive
    private Long productId;
    @NotNull @Min(1)
    private Integer quantity;
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
