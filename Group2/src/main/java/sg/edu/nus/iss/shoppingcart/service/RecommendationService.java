package sg.edu.nus.iss.shoppingcart.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.interceptor.CurrentUser;
import sg.edu.nus.iss.shoppingcart.repository.OrderItemRepository;
import sg.edu.nus.iss.shoppingcart.repository.ProductReviewRepository;
import java.time.LocalDate;
import java.util.*;

/** Explainable daily ranking for this small catalogue; filters are applied before ranking/pagination. */
@Service
public class RecommendationService {
    private final ProductService catalog;
    private final OrderItemRepository purchases;
    private final ProductReviewRepository reviews;
    private static final String HISTORY = "storeViewedCategories";
    public RecommendationService(ProductService catalog, OrderItemRepository purchases, ProductReviewRepository reviews) {
        this.catalog = catalog; this.purchases = purchases; this.reviews = reviews;
    }

    public void remember(HttpSession session, Product product) {
        synchronized (session) {
            Map<String, Integer> history = new HashMap<>(history(session));
            history.merge(product.getCategory(), 1, (oldValue, added) -> Math.min(20, oldValue + added));
            session.setAttribute(HISTORY, history);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> history(HttpSession session) {
        if (session == null) return Map.of();
        synchronized (session) {
            Object stored = session.getAttribute(HISTORY);
            return stored instanceof Map<?, ?> ? new HashMap<>((Map<String, Integer>) stored) : Map.of();
        }
    }

    @Transactional(readOnly = true)
    public List<Product> recommend(String category, String query, HttpSession session) {
        Map<String, Integer> affinity = new HashMap<>(history(session));
        Long userId = CurrentUser.getId(session);
        if (userId != null) {
            purchases.purchasesFor(userId).forEach(item -> affinity.merge(item.getProduct().getCategory(), 3, Integer::sum));
            reviews.findByUserId(userId).stream().filter(r -> r.getRating() >= 4)
                    .forEach(r -> affinity.merge(r.getProduct().getCategory(), r.getRating(), Integer::sum));
        }
        long day = LocalDate.now(java.time.ZoneId.of("Asia/Singapore")).toEpochDay();
        List<Product> result = new ArrayList<>(catalog.findAllProducts(category, query, "featured"));
        result.sort(Comparator.<Product>comparingDouble(p -> affinity.getOrDefault(p.getCategory(), 0) * 100.0
                + p.getAverageRating() * 3 + Math.log1p(p.getSalesCount())
                + new Random(day * 100003 + p.getId()).nextDouble() * 12).reversed().thenComparing(Product::getId));
        return result;
    }

    @Transactional(readOnly = true)
    public Page<Product> page(int page, int size, String category, String query, HttpSession session) {
        List<Product> all = recommend(category, query, session);
        int start = (int) Math.min((long) page * size, all.size());
        return new PageImpl<>(all.subList(start, Math.min(start + size, all.size())), PageRequest.of(page, size), all.size());
    }
}
