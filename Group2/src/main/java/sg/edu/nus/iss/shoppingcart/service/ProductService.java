package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Locale;
import sg.edu.nus.iss.shoppingcart.model.CatalogCategory;

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
        Sort sort = Sort.by("id").ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return productRepository.findByActiveTrue(pageable);
    }

    @Transactional(readOnly = true)
    public Page<Product> findProductPage(int page, int size, String category, String query, String order) {
        return productRepository.findAll(filters(category, query), PageRequest.of(page, size, sort(order)));
    }

    @Transactional(readOnly = true)
    public List<Product> findAllProducts(String category, String query, String order) {
        return productRepository.findAll(filters(category, query), sort(order));
    }

    @Transactional(readOnly = true)
    public List<CategorySummary> categories() {
        return CatalogCategory.ALL.stream().map(category -> new CategorySummary(category.slug(), category.name(),
                category.nameZh(), productRepository.count(filters(category.slug(), null)))).toList();
    }

    private Specification<Product> filters(String category, String query) {
        if (category != null && !category.isBlank() && !CatalogCategory.isValid(category)) {
            throw new IllegalArgumentException("Unknown product category");
        }
        if (query != null && query.length() > 100) throw new IllegalArgumentException("Search is too long");
        return (root, criteria, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.isTrue(root.get("active")));
            if (category != null && !category.isBlank()) {
                predicates.add(builder.equal(builder.coalesce(root.<String>get("category"), "workspace"), category));
            }
            if (query != null && !query.isBlank()) {
                String escaped = query.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\")
                        .replace("%", "\\%").replace("_", "\\_");
                String pattern = "%" + escaped + "%";
                predicates.add(builder.or(builder.like(builder.lower(root.get("name")), pattern, '\\'),
                        builder.like(builder.lower(root.get("nameZh")), pattern, '\\'),
                        builder.like(builder.lower(root.get("description")), pattern, '\\'),
                        builder.like(builder.lower(root.get("descriptionZh")), pattern, '\\'),
                        builder.like(builder.lower(root.get("brand")), pattern, '\\')));
            }
            return builder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private Sort sort(String order) {
        String selected = order == null || order.isBlank() ? "featured" : order;
        return switch (selected) {
            case "featured" -> Sort.by("id").ascending();
            case "price-asc" -> Sort.by("price").ascending().and(Sort.by("id"));
            case "price-desc" -> Sort.by("price").descending().and(Sort.by("id"));
            case "name" -> Sort.by("name").ascending().and(Sort.by("id"));
            case "newest" -> Sort.by("id").descending();
            default -> throw new IllegalArgumentException("Unknown product sort order");
        };
    }

    public record CategorySummary(String slug, String name, String nameZh, long count) {}

    @Transactional(readOnly = true)
    public Optional<Product> findProductById(Long id) {
        return productRepository.findByIdAndActiveTrue(id);
    }
}
