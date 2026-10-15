package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.data.domain.Page;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import java.util.List;
import java.util.Optional;

/**
 * Product operations following the classroom CourseService / CourseServiceImpl pattern.
 * @author 王重一
 */
public interface ProductService {
    List<Product> findAllProducts();

    Page<Product> findProductPage(int page, int size);

    Page<Product> findProductPage(int page, int size, String category, String query, String order);

    List<Product> findAllProducts(String category, String query, String order);

    List<Product> dailyDeals();

    List<CategorySummary> categories();

    Optional<Product> findProductById(Long id);

    Optional<Product> findForCheckout(Long id);

    record CategorySummary(String slug, String name, String nameZh, long count) { }
}
