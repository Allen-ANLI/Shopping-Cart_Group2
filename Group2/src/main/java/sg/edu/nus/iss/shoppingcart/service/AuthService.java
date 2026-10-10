package sg.edu.nus.iss.shoppingcart.service;

import jakarta.validation.Validator;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
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
    private final MessageSource messages;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       Validator validator, MessageSource messages) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.validator = validator;
        this.messages = messages;
    }

    @Transactional(readOnly = true)
    public Optional<User> authenticate(String username, String rawPassword) {
        return authenticate("username", username, rawPassword);
    }

    @Transactional(readOnly = true)
    public Optional<User> authenticate(String method, String identifier, String rawPassword) {
        if (identifier == null || identifier.isBlank() || identifier.length() > 255
                || rawPassword == null || rawPassword.isBlank()
                || rawPassword.getBytes(StandardCharsets.UTF_8).length > 72) {
            return Optional.empty();
        }
        Optional<User> account = switch (method == null ? "" : method) {
            case "username" -> userRepository.findByUsernameIgnoreCase(identifier.trim());
            case "email" -> uniqueAccount(userRepository.findAllByEmailIgnoreCase(identifier.trim()));
            case "phone" -> identifier.trim().matches("(?=(?:[^0-9]*[0-9]){6,15}[^0-9]*$)[+0-9][0-9 ()-]{5,29}")
                    ? uniqueAccount(userRepository.findAllByPhoneDigits(phoneDigits(identifier))) : Optional.empty();
            default -> Optional.empty();
        };
        return account
                .filter(user -> user.getPasswordHash() != null
                        && passwordEncoder.matches(rawPassword, user.getPasswordHash()));
    }

    private Optional<User> uniqueAccount(java.util.List<User> matches) {
        // Legacy data can contain shared contact details; never choose an arbitrary account.
        return matches.size() == 1 ? Optional.of(matches.get(0)) : Optional.empty();
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
            throw new BusinessException(message("auth.username.duplicate"));
        }
        requireAvailableContact(form.getEmail(), form.getPhone(), null);
        User user = new User(username, passwordEncoder.encode(form.getPassword()),
                form.getDisplayName(), form.getEmail());
        user.setRole(User.Role.CUSTOMER); // 服务器固定，绝不从表单读取角色。
        user.setPhone(form.getPhone());
        user.setBirthday(form.getBirthday());
        return userRepository.saveAndFlush(user);
    }

    @Transactional
    public void updateProfile(Long currentUserId, ProfileForm form) {
        validate(form);
        User user = findById(currentUserId)
                .orElseThrow(() -> new BusinessException(message("account.unavailable")));
        requireAvailableContact(form.getEmail(), form.getPhone(), currentUserId);
        user.setDisplayName(form.getDisplayName());
        user.setEmail(form.getEmail());
        user.setPhone(form.getPhone());
        user.setBirthday(form.getBirthday());
        userRepository.saveAndFlush(user);
    }

    private void requireAvailableContact(String email, String phone, Long currentUserId) {
        if (userRepository.findAllByEmailIgnoreCase(email).stream()
                .anyMatch(user -> !user.getId().equals(currentUserId))) {
            throw new BusinessException(message("account.email.duplicate"));
        }
        if (userRepository.findAllByPhoneDigits(phoneDigits(phone)).stream()
                .anyMatch(user -> !user.getId().equals(currentUserId))) {
            throw new BusinessException(message("account.phone.duplicate"));
        }
    }

    private static String phoneDigits(String phone) { return phone.replaceAll("[^0-9]", ""); }

    private <T> void validate(T form) {
        if (form == null) { throw new BusinessException(message("account.details.invalid")); }
        var violations = validator.validate(form);
        if (!violations.isEmpty()) {
            throw new BusinessException(violations.iterator().next().getMessage());
        }
    }

    private String message(String key) {
        return messages.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
