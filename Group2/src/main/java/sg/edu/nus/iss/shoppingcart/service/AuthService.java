package sg.edu.nus.iss.shoppingcart.service;

import jakarta.validation.Validator;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.form.ProfileForm;
import sg.edu.nus.iss.shoppingcart.form.RegisterForm;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

/**
 * 在 A 的 AuthService 上扩展 B，不另建认证框架或第二套用户表。
 * @author luopeiwen (B integration)
 */
@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final Validator validator;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       Validator validator) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
    }

    @Transactional(readOnly = true)
    public Optional<User> authenticate(String username, String rawPassword) {
        if (username == null || username.isBlank() || rawPassword == null || rawPassword.isBlank()
                || rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            return Optional.empty();
        }
        return userRepository.findByUsernameIgnoreCase(username.trim())
                .filter(user -> user.getPasswordHash() != null
                        && passwordEncoder.matches(rawPassword, user.getPasswordHash()));
    }

    @Transactional(readOnly = true)
    public Optional<User> findById(Long id) {
        return id == null || id <= 0 ? Optional.empty() : userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        return username == null ? Optional.empty()
                : userRepository.findByUsernameIgnoreCase(username.trim());
    }

    @Transactional
    public User register(RegisterForm form) {
        validate(form);
        String username = form.getUsername().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            throw new BusinessException("This username is already taken.");
        }
        User user = new User(username, passwordEncoder.encode(form.getPassword()),
                form.getDisplayName(), form.getEmail());
        user.setRole(User.Role.CUSTOMER); // 服务器固定，绝不从表单读取角色。
        return userRepository.saveAndFlush(user);
    }

    @Transactional
    public void updateProfile(Long currentUserId, ProfileForm form) {
        validate(form);
        User user = findById(currentUserId)
                .orElseThrow(() -> new BusinessException("The account is no longer available."));
        user.setDisplayName(form.getDisplayName());
        user.setEmail(form.getEmail());
        userRepository.saveAndFlush(user);
    }

    private <T> void validate(T form) {
        if (form == null) { throw new BusinessException("Please provide valid account details."); }
        var violations = validator.validate(form);
        if (!violations.isEmpty()) {
            throw new BusinessException(violations.iterator().next().getMessage());
        }
    }
}
