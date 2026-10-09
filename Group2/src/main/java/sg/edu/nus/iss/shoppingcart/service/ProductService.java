package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;

/**
 * 组织上架商品查询、分页排序和只读事务，供商品接口及其他模块调用。
 * @author 王重一
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Product> findAllProducts() {
        return productRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Page<Product> findProductPage(int page, int size) {
        return findProductPage(page, size, "", "featured");
    }

    /** Search is applied before pagination; ID breaks equal-price ties. */
    @Transactional(readOnly = true)
    public Page<Product> findProductPage(int page, int size, String query, String order) {
        String keyword = query == null ? "" : query.strip();
        if (keyword.length() > 100) { throw new IllegalArgumentException("Search is too long"); }
        Sort sort = switch (order) {
            case "featured" -> Sort.by("id").ascending();
            case "price-asc" -> Sort.by("price").ascending().and(Sort.by("id").ascending());
            case "price-desc" -> Sort.by("price").descending().and(Sort.by("id").ascending());
            default -> throw new IllegalArgumentException("Unknown product sort");
        };
        Pageable pageable = PageRequest.of(page, size, sort);
        return keyword.isEmpty() ? productRepository.findByActiveTrue(pageable)
                : productRepository.findByActiveTrueAndNameContainingIgnoreCase(keyword, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<Product> findProductById(Long id) {
        return productRepository.findByIdAndActiveTrue(id);
    }
}