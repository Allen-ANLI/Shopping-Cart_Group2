package sg.edu.nus.iss.shoppingcart.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import sg.edu.nus.iss.shoppingcart.entity.Product;

import java.util.List;
import java.util.Optional;

/**
 * 提供上架商品列表、分页查询及按商品 ID 查询的数据库访问接口。
 * @author 王重一
 */
public interface ProductRepository
        extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    List<Product> findByActiveTrue();

    Page<Product> findByActiveTrue(Pageable pageable);

    Optional<Product> findByIdAndActiveTrue(Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from Product p where p.id = :id")
    Optional<Product> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
