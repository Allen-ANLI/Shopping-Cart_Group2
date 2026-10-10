package sg.edu.nus.iss.shoppingcart.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.entity.CatalogSeedVersion;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.repository.CatalogSeedVersionRepository;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DataInitializer implements CommandLineRunner {
    public static final String CATALOG_VERSION = "catalog-40-bilingual-v1";
    private final ProductRepository products;
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final CatalogSeedVersionRepository versions;

    public DataInitializer(ProductRepository products, UserRepository users,
                           PasswordEncoder passwords, CatalogSeedVersionRepository versions) {
        this.products = products; this.users = users; this.passwords = passwords; this.versions = versions;
    }

    @Override @Transactional
    public void run(String... args) {
        if (!versions.existsById(CATALOG_VERSION)) {
            Map<String, Product> existing = products.findAll().stream().collect(Collectors.toMap(
                    Product::getName, Function.identity(), (first, second) -> first));
            List<Product> added = new ArrayList<>();
            for (Product seed : CatalogSeedProducts.all()) {
                Product original = existing.get(seed.getName());
                if (original == null) added.add(seed);
                else original.fillMissingCatalogMetadata(seed);
            }
            products.saveAll(added);
            // Same transaction: later startups never replenish removed or hidden products.
            versions.save(new CatalogSeedVersion(CATALOG_VERSION));
        }
        createUserIfMissing("alice", "demo123", "Alice");
        createUserIfMissing("bob", "demo123", "Bob");
    }

    private void createUserIfMissing(String username, String password, String displayName) {
        if (users.findByUsername(username).isPresent()) return;
        User user = new User();
        user.setUsername(username); user.setDisplayName(displayName);
        user.setPasswordHash(passwords.encode(password));
        users.save(user);
    }
}
