package sg.edu.nus.iss.shoppingcart.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 验证商品示例初始化、重复启动及已有商品和用户数据的保留行为。
 * @author 王重一
 */
class DataInitializerTests {
    private ProductRepository products;
    private UserRepository users;
    private PasswordEncoder passwords;
    private DataInitializer initializer;
    private final List<Product> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        products = mock(ProductRepository.class);
        users = mock(UserRepository.class);
        passwords = mock(PasswordEncoder.class);
        when(users.findByUsername(anyString())).thenReturn(Optional.of(new User()));
        when(products.count()).thenAnswer(invocation -> (long) saved.size());
        when(products.save(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            saved.add(product);
            return product;
        });
        when(products.saveAll(any())).thenAnswer(invocation -> {
            Iterable<Product> batch = invocation.getArgument(0);
            batch.forEach(saved::add);
            return saved;
        });
        initializer = new DataInitializer(products, users, passwords);
    }

    @Test
    void initializesTwelveCompleteAndDistinctProductsInAnEmptyDatabase() {
        initializer.run();
        assertEquals(12, saved.size());
        assertEquals("Keyboard", saved.get(0).getName());
        assertEquals("Mouse", saved.get(1).getName());
        HashSet<String> names = new HashSet<>();
        for (Product product : saved) {
            assertTrue(names.add(product.getName()), "Duplicate product: " + product.getName());
            assertNotNull(product.getDescription());
            assertFalse(product.getDescription().isBlank());
            assertTrue(product.getPrice().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(2, product.getPrice().scale());
            assertTrue(product.getImageUrl().startsWith("/images/"));
            assertTrue(product.isActive());
        }
    }

    @Test
    void doesNotOverwriteExistingProductFieldsOrReenableInactiveProducts() {
        Product existing = new Product();
        existing.setName("My edited product");
        existing.setPrice(new BigDecimal("17.25"));
        existing.setImageUrl("/images/my-custom-image.svg");
        existing.setActive(false);
        saved.add(existing);
        initializer.run();
        assertEquals(1, saved.size());
        assertEquals("My edited product", existing.getName());
        assertEquals(new BigDecimal("17.25"), existing.getPrice());
        assertEquals("/images/my-custom-image.svg", existing.getImageUrl());
        assertFalse(existing.isActive());
        verify(products, never()).save(any(Product.class));
        verify(products, never()).saveAll(any());
    }

    @Test
    void doesNotDuplicateProductsOnTheNextStartup() {
        initializer.run();
        initializer.run();
        assertEquals(12, saved.size());
    }

    @Test
    void leavesExistingDemoUsersAndPasswordsUntouched() {
        initializer.run();
        verify(users, never()).save(any(User.class));
        verifyNoInteractions(passwords);
    }
}