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

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 0")
    private int discountPercent;

    // SQL expression keeps paged price ordering aligned with the price paid at checkout.
    @org.hibernate.annotations.Formula("greatest(round(price * (100 - discount_percent) / 100.0, 2), 0.01)")
    private BigDecimal effectivePriceValue;

    public int getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(int discountPercent) {
        if (discountPercent < 0 || discountPercent > 99) {
            throw new IllegalArgumentException("Discount must be between 0 and 99 percent");
        }
        this.discountPercent = discountPercent;
    }
    public BigDecimal getOriginalPrice() { return price; }
    public BigDecimal getEffectivePrice() {
        if (price == null) return null;
        return price.multiply(BigDecimal.valueOf(100 - discountPercent))
                .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP).max(new BigDecimal("0.01"));
    }
    public boolean isOnSale() { return discountPercent > 0; }

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(
            nullable = false,
            columnDefinition = "BOOLEAN DEFAULT TRUE"
    )
    private boolean active = true;

    @Column(nullable = false, columnDefinition = "INTEGER DEFAULT 100")
    private int stockQuantity = 100;

    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean hideWhenOutOfStock;

    // Derived from persisted reviews/orders, so edits and purchases cannot drift from the catalogue.
    @org.hibernate.annotations.Formula("(select coalesce(avg(1.0 * r.rating), 0) from product_reviews r where r.product_id = id)")
    private double averageRating;
    @org.hibernate.annotations.Formula("(select count(*) from product_reviews r where r.product_id = id)")
    private long totalReviews;
    @org.hibernate.annotations.Formula("(select coalesce(sum(i.quantity), 0) from order_items i join orders o on o.id = i.order_id where i.product_id = id and (o.payment_status = 'PAID' or o.payment_status is null))")
    private long salesCount;

    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public boolean isHideWhenOutOfStock() { return hideWhenOutOfStock; }
    public void setHideWhenOutOfStock(boolean value) { hideWhenOutOfStock = value; }
    public boolean isVisible() { return active && (!hideWhenOutOfStock || stockQuantity > 0); }
    public double getAverageRating() { return Math.round(averageRating * 10) / 10.0; }
    public long getTotalReviews() { return totalReviews; }
    public long getSalesCount() { return salesCount; }

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
    public String getBrand() { return fallback(brand, "NEXUS Essentials"); }
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
