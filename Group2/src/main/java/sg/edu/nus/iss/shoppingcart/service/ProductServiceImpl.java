package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.model.CatalogCategory;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Service handles parameters and transactions; Repository runs the JPQL queries.
 * @author 王重一
 */
@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public List<Product> findAllProducts() {
        return findAllProducts(null, null, "featured");
    }

    @Override
    public Page<Product> findProductPage(int page, int size) {
        return findProductPage(page, size, null, null, "featured");
    }

    @Override
    public Page<Product> findProductPage(int page, int size, String category, String query, String order) {
        validateFilters(category, query);
        return productRepository.findVisibleProducts(categoryFilter(category), searchPattern(query),
                PageRequest.of(page, size, sort(order)));
    }

    @Override
    public List<Product> findAllProducts(String category, String query, String order) {
        validateFilters(category, query);
        return productRepository.findVisibleProducts(categoryFilter(category), searchPattern(query), sort(order));
    }

    @Override
    public List<Product> dailyDeals() {
        Sort sort = Sort.by("discountPercent").descending()
                .and(Sort.by("salesCount").descending()).and(Sort.by("id"));
        return productRepository.findVisibleDeals(sort);
    }

    @Override
    public List<CategorySummary> categories() {
        List<CategorySummary> summaries = new ArrayList<>();
        for (CatalogCategory category : CatalogCategory.ALL) {
            long count = productRepository.countVisibleProductsByCategory(category.slug());
            summaries.add(new CategorySummary(category.slug(), category.name(), category.nameZh(), count));
        }
        return summaries;
    }

    @Override
    public Optional<Product> findProductById(Long id) {
        return productRepository.findByIdAndActiveTrue(id).filter(Product::isVisible);
    }

    @Override
    @Transactional
    public Optional<Product> findForCheckout(Long id) {
        return productRepository.findForUpdate(id).filter(Product::isActive);
    }

    private void validateFilters(String category, String query) {
        if (category != null && !category.isBlank() && !CatalogCategory.isValid(category)) {
            throw new IllegalArgumentException("Unknown product category");
        }
        if (query != null && query.length() > 100) {
            throw new IllegalArgumentException("Search is too long");
        }
    }

    private String categoryFilter(String category) {
        return category == null || category.isBlank() ? null : category;
    }

    private String searchPattern(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        // Search punctuation literally, as in the original catalogue query.
        String escaped = query.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private Sort sort(String order) {
        String selected = order == null || order.isBlank() ? "featured" : order;
        switch (selected) {
            case "featured":
                return Sort.by("id").ascending();
            case "price-asc":
                return Sort.by("effectivePriceValue").ascending().and(Sort.by("id"));
            case "price-desc":
                return Sort.by("effectivePriceValue").descending().and(Sort.by("id"));
            case "sales":
                return Sort.by("salesCount").descending().and(Sort.by("id"));
            case "rating":
                return Sort.by("averageRating").descending().and(Sort.by("totalReviews").descending())
                        .and(Sort.by("id"));
            default:
                throw new IllegalArgumentException("Unknown product sort order");
        }
    }
}
