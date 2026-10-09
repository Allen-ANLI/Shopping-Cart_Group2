package sg.edu.nus.iss.shoppingcart.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;

/**
 * 仅在隔离的 b-demo 环境新增管理员；不改正式库、不重置任何现有密码或角色。
 * @author luopeiwen
 */
@Component
@Profile("b-demo")
public class BDemoUserInitializer implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    public BDemoUserInitializer(UserRepository users, PasswordEncoder encoder) {
        this.users = users;
        this.encoder = encoder;
    }
    @Override
    @Transactional
    public void run(String... args) {
        if (users.findByUsernameIgnoreCase("admin").isPresent()) { return; }
        User user = new User("admin", encoder.encode("admin123"), "Administrator", "admin@example.test");
        user.setRole(User.Role.ADMIN);
        users.save(user);
    }
}
