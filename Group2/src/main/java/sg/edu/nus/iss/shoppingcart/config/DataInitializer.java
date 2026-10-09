package sg.edu.nus.iss.shoppingcart.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * 提供启动数据初始化入口；王重一负责其中的商品示例初始化部分。
 * @author 王重一
 */
@Component
public class DataInitializer implements CommandLineRunner {
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            ProductRepository productRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        // Preserve existing products, including administrator edits and inactive items.
        if (productRepository.count() == 0) {
            productRepository.saveAll(List.of(
                    createProduct("Keyboard",
                            "A comfortable keyboard for everyday use.",
                            "50.00", "/images/keyboard.png"),
                    createProduct("Mouse",
                            "A wireless mouse for work and study.",
                            "20.00", "/images/mouse.png"),
                    createProduct("Headphones",
                            "Comfortable over-ear headphones for music, online classes and focused work.",
                            "79.00", "/images/headphones.svg"),
                    createProduct("USB-C Hub",
                            "A compact multi-port USB-C hub for connecting everyday desk accessories.",
                            "39.90", "/images/usb-c-hub.svg"),
                    createProduct("Laptop Stand",
                            "An aluminium stand that raises your laptop for a more comfortable desk setup.",
                            "35.00", "/images/laptop-stand.svg"),
                    createProduct("Desk Lamp",
                            "A warm LED desk lamp with an adjustable arm for reading and evening study.",
                            "29.00", "/images/desk-lamp.svg"),
                    createProduct("Webcam",
                            "A compact HD webcam for video calls, group meetings and online lessons.",
                            "59.90", "/images/webcam.svg"),
                    createProduct("Portable SSD",
                            "A slim portable solid-state drive for keeping documents and project files handy.",
                            "99.90", "/images/portable-ssd.svg"),
                    createProduct("Monitor",
                            "A clear widescreen monitor that gives your workspace more room for everyday tasks.",
                            "199.90", "/images/monitor.svg"),
                    createProduct("Bluetooth Speaker",
                            "A small wireless speaker for music and podcasts during your breaks.",
                            "49.90", "/images/bluetooth-speaker.svg"),
                    createProduct("Desk Mat",
                            "A soft desk mat that keeps your keyboard and mouse area comfortable and tidy.",
                            "12.90", "/images/desk-mat.svg"),
                    createProduct("USB Microphone",
                            "A desktop USB microphone for clear voice recordings and online conversations.",
                            "89.90", "/images/usb-microphone.svg")
            ));
        }
        createUserIfMissing("alice", "demo123", "Alice");
        createUserIfMissing("bob", "demo123", "Bob");
    }

    private Product createProduct(
            String name, String description, String price, String imageUrl) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setPrice(new BigDecimal(price));
        product.setImageUrl(imageUrl);
        return product;
    }

    private void createUserIfMissing(
            String username, String rawPassword, String displayName) {
        if (userRepository.findByUsername(username).isPresent()) {
            return;
        }
        User user = new User();
        user.setUsername(username);
        user.setDisplayName(displayName);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        userRepository.save(user);
    }
}