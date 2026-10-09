package sg.edu.nus.iss.shoppingcart.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sg.edu.nus.iss.shoppingcart.entity.Product;

import java.util.List;
import java.util.Optional;

/**
 * 提供上架商品列表、分页查询及按商品 ID 查询的数据库访问接口。
 * @author 王重一
 */
public interface ProductRepository
        extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrue();

    Page<Product> findByActiveTrue(Pageable pageable);

    Optional<Product> findByIdAndActiveTrue(Long id);
}