package sg.edu.nus.iss.shoppingcart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * C 购物车表单。0 在更新操作中表示移除；新增时服务层要求至少 1 件。
 * @author Letian Xie
 */
public class CartQuantityForm {
    @NotNull @Positive
    private Long productId;
    @NotNull @Min(0) @Max(99)
    private Integer quantity;
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
