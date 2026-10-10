package sg.edu.nus.iss.shoppingcart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sg.edu.nus.iss.shoppingcart.entity.Product;

/**
 * 商品查询接口 —— E 的管理员后台专用。
 *
 * <p><b>与 A 的 {@code ProductRepository} 的分工：</b>
 * A 维护面向顾客的查询（只看上架商品、分页、关键词）。
 * 这个接口服务后台，需要看到<b>全部</b>商品包括已下架的，
 * 并且要能回答"这个商品有没有被订单引用过"。
 * 两者用途不同，方法不重叠，可以共存。</p>
 *
 * @author 蔡千一（Module E）
 */
public interface AdminProductQueryRepository extends JpaRepository<Product, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    java.util.Optional<Product> findForUpdate(@Param("id") Long id);

    /**
     * 判断某个商品是否已经被订单明细引用。
     *
     * <p>这是"不物理删除被订单引用的商品"这条规则的依据。
     * 被引用过的商品只能下架：明细表上有指向 products 的外键，
     * 物理删除会让历史订单保存失败，或者留下悬空引用。</p>
     *
     * <p>用 {@code exists} 而不是 {@code count}，查到第一条就返回。</p>
     *
     * @param productId 商品 ID
     * @return true 表示至少有一条订单明细指向它
     */
    @Query("SELECT CASE WHEN COUNT(i) > 0 THEN true ELSE false END "
            + "FROM OrderItem i WHERE i.product.id = :productId")
    boolean isReferencedByAnyOrder(@Param("productId") Long productId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM ProductReview r WHERE r.product.id = :productId")
    boolean isReferencedByAnyReview(@Param("productId") Long productId);
}
