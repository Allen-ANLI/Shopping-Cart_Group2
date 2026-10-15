package sg.edu.nus.iss.shoppingcart.service;

import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.form.ProfileForm;
import sg.edu.nus.iss.shoppingcart.form.PasswordChangeForm;
import sg.edu.nus.iss.shoppingcart.form.RegisterForm;
import java.util.Optional;

/** Business operations, following the CourseService/Impl tutorial. */
public interface AuthService {
    Optional<User> authenticate(String username, String rawPassword);

    Optional<User> authenticate(String method, String identifier, String rawPassword);

    Optional<User> findById(Long id);

    Optional<User> findByUsername(String username);

    User register(RegisterForm form);

    void updateProfile(Long currentUserId, ProfileForm form);

    void changePassword(Long currentUserId, PasswordChangeForm form);
}
