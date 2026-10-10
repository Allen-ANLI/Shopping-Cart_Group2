package sg.edu.nus.iss.shoppingcart.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sg.edu.nus.iss.shoppingcart.entity.User;
import java.util.Optional;
import java.util.List;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 保留 A/D 使用的接口，在同一 app_users 表上扩展 B 的用户名查询。
 * @author luopeiwen (B integration)
 */
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);

    List<User> findAllByEmailIgnoreCase(String email);

    // Existing accounts may contain spaces or punctuation in their saved phone number.
    @Query("select u from User u where replace(replace(replace(replace(replace(u.phone, ' ', ''), '+', ''), '-', ''), '(', ''), ')', '') = :digits")
    List<User> findAllByPhoneDigits(@Param("digits") String digits);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id")
    Optional<User> lockById(@Param("id") Long id);
}
