package sg.edu.nus.iss.shoppingcart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

/**
 * 描述商品及数据库映射，保存名称、描述、价格、图片地址和上架状态。
 * @author 王重一
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 1000)
    private String description;

    // Nullable additions let Hibernate update an existing catalogue without data loss.
    @Column(length = 30)
    private String category;

    @Column(length = 80)
    private String brand;

    @Column(length = 100)
    private String origin;

    @Column(name = "name_zh", length = 100)
    private String nameZh;

    @Column(name = "description_zh", length = 1000)
    private String descriptionZh;

    @Column(name = "origin_zh", length = 100)
    private String originZh;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(
            nullable = false,
            columnDefinition = "BOOLEAN DEFAULT TRUE"
    )
    private boolean active = true;

    public Product() {
    }

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

    public String getCategory() { return fallback(category, "workspace"); }
    public void setCategory(String category) { this.category = category; }
    public String getBrand() { return fallback(brand, "Group2 Essentials"); }
    public void setBrand(String brand) { this.brand = brand; }
    public String getOrigin() { return fallback(origin, "Singapore"); }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getNameZh() { return fallback(nameZh, name); }
    public void setNameZh(String nameZh) { this.nameZh = nameZh; }
    public String getDescriptionZh() { return fallback(descriptionZh, description); }
    public void setDescriptionZh(String descriptionZh) { this.descriptionZh = descriptionZh; }
    public String getOriginZh() { return fallback(originZh, getOrigin()); }
    public void setOriginZh(String originZh) { this.originZh = originZh; }

    /** Used only by the one-time catalogue migration. Existing populated fields are retained. */
    public void fillMissingCatalogMetadata(Product seed) {
        if (category == null || category.isBlank()) category = seed.category;
        if (brand == null || brand.isBlank()) brand = seed.brand;
        if (origin == null || origin.isBlank()) origin = seed.origin;
        if (nameZh == null || nameZh.isBlank()) nameZh = seed.nameZh;
        // Do not attach a seed translation to administrator-edited information.
        if ((descriptionZh == null || descriptionZh.isBlank()) && java.util.Objects.equals(description, seed.description)) {
            descriptionZh = seed.descriptionZh;
        }
        if ((originZh == null || originZh.isBlank()) && java.util.Objects.equals(origin, seed.origin)) originZh = seed.originZh;
    }

    private String fallback(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

}
