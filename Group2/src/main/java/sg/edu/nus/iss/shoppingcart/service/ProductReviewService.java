package sg.edu.nus.iss.shoppingcart.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.entity.ProductReview;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.exception.NotAuthenticatedException;
import sg.edu.nus.iss.shoppingcart.exception.ResourceNotFoundException;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import sg.edu.nus.iss.shoppingcart.repository.ProductReviewRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProductReviewService {
    private final ProductRepository products;
    private final ProductReviewRepository reviews;
    private final UserRepository users;
    private final EntityManager entityManager;

    public ProductReviewService(ProductRepository products, ProductReviewRepository reviews,
                                UserRepository users, EntityManager entityManager) {
        this.products = products; this.reviews = reviews; this.users = users; this.entityManager = entityManager;
    }

    @Transactional(readOnly = true)
    public ReviewSummary read(Long productId, Long userId) {
        requireProduct(productId);
        return summary(productId, userId);
    }

    @Transactional
    public ReviewSummary save(Long productId, Long userId, int rating, String comment) {
        if (userId == null) throw new NotAuthenticatedException();
        if (rating < 1 || rating > 5 || comment == null || comment.trim().length() < 5
                || comment.trim().length() > 1000) throw new IllegalArgumentException("Please check the information you entered");
        // Serialise reviews by the same account even when it uses more than one session.
        User user = entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (user == null) throw new NotAuthenticatedException();
        Product product = requireProduct(productId);
        ProductReview review = reviews.findByProductIdAndUserId(productId, userId).orElseGet(ProductReview::new);
        review.setProduct(product);
        review.setUser(user);
        review.setRating(rating);
        review.setComment(comment.trim());
        review.setCreatedAt(LocalDateTime.now());
        reviews.saveAndFlush(review);
        return summary(productId, userId);
    }

    private Product requireProduct(Long id) {
        return products.findByIdAndActiveTrue(id).orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }

    private ReviewSummary summary(Long productId, Long userId) {
        List<ProductReview> stored = reviews.findByProductIdOrderByCreatedAtDescIdDesc(productId);
        List<ReviewItem> items = stored.stream().map(review -> new ReviewItem(review.getId(),
                review.getUser().getDisplayName(), review.getRating(), review.getComment(), review.getCreatedAt())).toList();
        OwnReview own = stored.stream().filter(review -> review.getUser().getId().equals(userId))
                .map(review -> new OwnReview(review.getRating(), review.getComment())).findFirst().orElse(null);
        double average = Math.round(stored.stream().mapToInt(ProductReview::getRating).average().orElse(0) * 10) / 10.0;
        return new ReviewSummary(items, average, items.size(), userId != null && users.existsById(userId), own);
    }

    public record ReviewItem(Long id, String displayName, int rating, String comment, LocalDateTime createdAt) {}
    public record OwnReview(int rating, String comment) {}
    public record ReviewSummary(List<ReviewItem> reviews, double averageRating, long totalReviews,
                                boolean canReview, OwnReview ownReview) {}
}
