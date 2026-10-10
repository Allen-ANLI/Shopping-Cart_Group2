package sg.edu.nus.iss.shoppingcart.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import sg.edu.nus.iss.shoppingcart.entity.CatalogSeedVersion;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.repository.CatalogSeedVersionRepository;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DataInitializerTests {
    private ProductRepository products;
    private UserRepository users;
    private PasswordEncoder passwords;
    private CatalogSeedVersionRepository versions;
    private DataInitializer initializer;
    private final List<Product> saved = new ArrayList<>();
    private final Set<String> applied = new HashSet<>();

    @BeforeEach void setUp() {
        products = mock(ProductRepository.class);
        users = mock(UserRepository.class);
        passwords = mock(PasswordEncoder.class);
        versions = mock(CatalogSeedVersionRepository.class);
        when(users.findByUsername(anyString())).thenReturn(Optional.of(new User()));
        when(products.findAll()).thenAnswer(invocation -> List.copyOf(saved));
        when(products.saveAll(any())).thenAnswer(invocation -> {
            Iterable<Product> batch = invocation.getArgument(0);
            batch.forEach(saved::add);
            return saved;
        });
        when(versions.existsById(anyString())).thenAnswer(invocation -> applied.contains(invocation.getArgument(0)));
        when(versions.save(any(CatalogSeedVersion.class))).thenAnswer(invocation -> {
            CatalogSeedVersion version = invocation.getArgument(0);
            applied.add(version.getVersion());
            return version;
        });
        initializer = new DataInitializer(products, users, passwords, versions);
    }

    @Test void initializesFortyDistinctBilingualProductsTenPerCategory() {
        initializer.run();
        assertEquals(40, saved.size());
        assertEquals("Keyboard", saved.get(0).getName());
        assertEquals("Mouse", saved.get(1).getName());
        assertEquals(Map.of("computing", 10L, "typing", 10L, "workspace", 10L, "audio", 10L),
                saved.stream().collect(Collectors.groupingBy(Product::getCategory, Collectors.counting())));
        Set<String> names = new HashSet<>();
        for (Product product : saved) {
            assertTrue(names.add(product.getName()));
            assertFalse(product.getDescription().isBlank());
            assertNotEquals(product.getName(), product.getNameZh());
            assertNotEquals(product.getDescription(), product.getDescriptionZh());
            assertFalse(product.getBrand().isBlank());
            assertFalse(product.getOrigin().isBlank());
            assertTrue(product.getPrice().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(2, product.getPrice().scale());
            assertTrue(product.getImageUrl().startsWith("/images/"));
            assertTrue(product.isActive());
        }
        assertTrue(applied.contains(DataInitializer.CATALOG_VERSION));
    }

    @Test void migrationPreservesExistingProductFieldsAndInactiveState() {
        Product existing = new Product();
        existing.setName("Keyboard");
        existing.setDescription("Administrator edited description");
        existing.setPrice(new BigDecimal("17.25"));
        existing.setImageUrl("/images/custom.svg");
        existing.setBrand("Custom brand");
        existing.setCategory("audio");
        existing.setOrigin("My warehouse");
        existing.setActive(false);
        saved.add(existing);
        initializer.run();
        assertEquals(40, saved.size());
        assertEquals("Keyboard", existing.getName());
        assertEquals("Administrator edited description", existing.getDescription());
        assertEquals(new BigDecimal("17.25"), existing.getPrice());
        assertEquals("/images/custom.svg", existing.getImageUrl());
        assertEquals("Custom brand", existing.getBrand());
        assertEquals("audio", existing.getCategory());
        assertEquals("My warehouse", existing.getOrigin());
        assertEquals("My warehouse", existing.getOriginZh());
        assertEquals("Administrator edited description", existing.getDescriptionZh());
        assertFalse(existing.isActive());
        assertEquals(1, saved.stream().filter(p -> p.getName().equals("Keyboard")).count());
    }

    @Test void preservesUnrelatedProductsDuringMigration() {
        Product custom = new Product();
        custom.setName("My own item"); custom.setPrice(new BigDecimal("6.00"));
        saved.add(custom);
        initializer.run();
        assertEquals(41, saved.size());
        assertSame(custom, saved.get(0));
    }

    @Test void restartDoesNotDuplicateOrRestoreRemovedAndHiddenProducts() {
        initializer.run();
        saved.remove(0);
        saved.get(0).setActive(false);
        saved.get(0).setName("Edited mouse");
        initializer.run();
        assertEquals(39, saved.size());
        assertFalse(saved.get(0).isActive());
        assertEquals("Edited mouse", saved.get(0).getName());
        verify(products, times(1)).saveAll(any());
        saved.clear();
        initializer.run();
        assertTrue(saved.isEmpty(), "A completed migration must not repopulate even an empty catalogue");
    }

    @Test void existingDemoUsersAndPasswordsRemainUntouched() {
        initializer.run();
        verify(users, never()).save(any(User.class));
        verifyNoInteractions(passwords);
    }
}
