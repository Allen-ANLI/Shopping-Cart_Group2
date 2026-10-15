package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.data.domain.Page;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.dto.ProductForm;
import java.math.BigDecimal;

/** Business operations, following the CourseService/Impl tutorial. */
public interface AdminProductService {
    int PAGE_SIZE = 10;

    Page<Product> findAllForAdmin(int page);

    Product findById(Long id);

    Product create(String name, String description,
                          BigDecimal price, String imageUrl);

    Product create(String name, String description,
                          BigDecimal price, String imageUrl, boolean active);

    Product create(ProductForm form);

    Product update(Long id, ProductForm form);

    Product update(Long id, String name, String description,
                          BigDecimal price, String imageUrl, boolean active);

    Product toggleActive(Long id);

    void deleteIfUnreferenced(Long id);

    long countAll();

    long countInactive();
}
