package sg.edu.nus.iss.shoppingcart.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import jakarta.persistence.LockModeType;
import sg.edu.nus.iss.shoppingcart.entity.Product;

import java.util.List;
import java.util.Optional;

/**
 * 提供上架商品列表、分页查询及按商品 ID 查询的数据库访问接口。
 * @author 王重一
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // JPQL and named parameters follow the classroom JPQL tutorials.
    String VISIBLE_FILTER = """
            p.active = true
            AND (p.hideWhenOutOfStock = false OR p.stockQuantity > 0)
            AND (:category IS NULL OR COALESCE(p.category, 'workspace') = :category)
            AND (:search IS NULL
                OR LOWER(p.name) LIKE :search ESCAPE '\\'
                OR LOWER(p.nameZh) LIKE :search ESCAPE '\\'
                OR LOWER(p.brand) LIKE :search ESCAPE '\\')
            """;

    @Query(value = "SELECT p FROM Product p WHERE " + VISIBLE_FILTER,
            countQuery = "SELECT COUNT(p) FROM Product p WHERE " + VISIBLE_FILTER)
    Page<Product> findVisibleProducts(@Param("category") String category,
                                      @Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE " + VISIBLE_FILTER)
    List<Product> findVisibleProducts(@Param("category") String category,
                                      @Param("search") String search, Sort sort);

    @Query("""
            SELECT p FROM Product p
            WHERE p.active = true AND p.discountPercent > 0 AND p.stockQuantity > 0
            """)
    List<Product> findVisibleDeals(Sort sort);

    @Query("""
            SELECT COUNT(p) FROM Product p
            WHERE p.active = true
              AND (p.hideWhenOutOfStock = false OR p.stockQuantity > 0)
              AND COALESCE(p.category, 'workspace') = :category
            """)
    long countVisibleProductsByCategory(@Param("category") String category);

    List<Product> findByActiveTrue();

    Page<Product> findByActiveTrue(Pageable pageable);

    Optional<Product> findByIdAndActiveTrue(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findForUpdate(@Param("id") Long id);
}
