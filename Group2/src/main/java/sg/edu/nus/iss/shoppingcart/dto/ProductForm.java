package sg.edu.nus.iss.shoppingcart.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 管理员后台的商品表单。
 *
 * <p>同时用于新增和编辑两种场景：{@code id} 为空走新增，
 * 有值走编辑，由 Controller 判断。</p>
 *
 * <p><b>校验必须在服务端做。</b>页面把输入框禁用或隐藏只是界面行为，
 * 任何人绕过页面直接 POST 也能提交任意数据。这里用 Bean Validation
 * 注解声明规则，配合 {@code @Valid} 生效，非法输入会被拒绝并回显提示。</p>
 *
 * @author 蔡千一（Module E）
 */
public class ProductForm {

    /** 商品 ID，新增时为空。 */
    private Long id;

    /**
     * 商品名称。
     *
     * <p>用 {@code @NotBlank} 而不是 {@code @NotNull}：
     * 后者只拦 null，空字符串 {@code ""} 和纯空格都能通过。
     * 商品名必填的场景必须用 @NotBlank。</p>
     */
    @NotBlank(message = "Product name is required")
    @Size(max = 100, message = "Product name must not exceed 100 characters")
    private String name;

    /** 商品描述，可留空。 */
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    /**
     * 商品单价。
     *
     * <p>{@code @DecimalMin("0.01")} 拒绝零和负数——
     * 分工文档的验收标准是"价格等非法输入被后端拒绝"。
     * {@code @Digits} 限制整数 4 位、小数 2 位，
     * 和数据库 {@code DECIMAL(12,2)} 对得上，避免超大数导致入库失败。</p>
     */
    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    @Digits(integer = 8, fraction = 2, message = "Price must have at most 2 decimal places")
    private BigDecimal price;

    /**
     * 商品图片地址。
     *
     * <p>分工文档明确后台"先使用图片地址，不增加图片上传"，
     * 所以这里只是一个文本框，没有文件上传控件。</p>
     *
     * <p>只做长度校验，不校验URL 格式：允许留空，
     * 也允许填相对路径（项目里的静态图）或外部链接。</p>
     */
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    /** 是否上架。 */
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}