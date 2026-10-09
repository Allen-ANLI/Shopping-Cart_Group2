package sg.edu.nus.iss.shoppingcart.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Shared password encoder supplied in the A project.
 * Original implementation retained; F verified the integrated delivery.
 * @author Group 2 (original shared project)
 * @author 李岸 Li An (F integration and delivery)
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}