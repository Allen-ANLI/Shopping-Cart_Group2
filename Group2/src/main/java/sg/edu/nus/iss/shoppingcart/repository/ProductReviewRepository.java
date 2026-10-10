package sg.edu.nus.iss.shoppingcart.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import sg.edu.nus.iss.shoppingcart.entity.ProductReview;
import java.util.List;
import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {
    @EntityGraph(attributePaths = "user")
    List<ProductReview> findByProductIdOrderByCreatedAtDescIdDesc(Long productId);
    Optional<ProductReview> findByProductIdAndUserId(Long productId, Long userId);
}
