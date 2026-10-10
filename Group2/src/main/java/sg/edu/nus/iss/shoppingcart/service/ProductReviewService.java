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
    private final sg.edu.nus.iss.shoppingcart.repository.OrderItemRepository purchases;

    public ProductReviewService(ProductRepository products, ProductReviewRepository reviews,
                                UserRepository users, EntityManager entityManager,
                                sg.edu.nus.iss.shoppingcart.repository.OrderItemRepository purchases) {
        this.products = products; this.reviews = reviews; this.users = users; this.entityManager = entityManager;
        this.purchases = purchases;
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
                || comment.trim().length() > 1000) throw new IllegalArgumentException(PaymentService.localized("Choose 1–5 stars and write a review of 5–1000 characters.", "请选择 1–5 星，并填写 5–1000 个字符的评价。"));
        // Serialise reviews by the same account even when it uses more than one session.
        User user = entityManager.find(User.class, userId, LockModeType.PESSIMISTIC_WRITE);
        if (user == null) throw new NotAuthenticatedException();
        Product product = products.findById(productId).orElseThrow(() -> ResourceNotFoundException.of("Product", productId));
        if (!purchases.hasConfirmedPurchase(userId, productId)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    PaymentService.localized("Confirm receipt in your order before writing a review.", "请先在订单详情中确认收货，再进行评价。"));
        }
        ProductReview review = reviews.findByProductIdAndUserId(productId, userId).orElseGet(ProductReview::new);
        review.setProduct(product);
        review.setUser(user);
        review.setRating(rating);
        review.setComment(comment.trim());
        review.setCreatedAt(LocalDateTime.now());
        reviews.saveAndFlush(review);
        return summary(productId, userId);
    }

    @Transactional(readOnly = true)
    public OwnReview ownReview(Long productId, Long userId) {
        return reviews.findByProductIdAndUserId(productId, userId)
                .map(review -> new OwnReview(review.getRating(), review.getComment())).orElse(null);
    }

    private Product requireProduct(Long id) {
        return products.findByIdAndActiveTrue(id).filter(Product::isVisible)
                .orElseThrow(() -> ResourceNotFoundException.of("Product", id));
    }

    private ReviewSummary summary(Long productId, Long userId) {
        List<ProductReview> stored = reviews.findByProductIdOrderByCreatedAtDescIdDesc(productId);
        List<ReviewItem> items = stored.stream().map(review -> new ReviewItem(review.getId(),
                review.getUser().getDisplayName(), review.getRating(), review.getComment(), review.getCreatedAt(), review.isSample())).toList();
        OwnReview own = stored.stream().filter(review -> review.getUser().getId().equals(userId))
                .map(review -> new OwnReview(review.getRating(), review.getComment())).findFirst().orElse(null);
        double average = Math.round(stored.stream().mapToInt(ProductReview::getRating).average().orElse(0) * 10) / 10.0;
        boolean loggedIn = userId != null && users.existsById(userId);
        return new ReviewSummary(items, average, items.size(), loggedIn && purchases.hasConfirmedPurchase(userId, productId), own, loggedIn);
    }

    public record ReviewItem(Long id, String displayName, int rating, String comment, LocalDateTime createdAt, boolean sample) {}
    public record OwnReview(int rating, String comment) {}
    public record ReviewSummary(List<ReviewItem> reviews, double averageRating, long totalReviews,
                                boolean canReview, OwnReview ownReview, boolean loggedIn) {}
}
