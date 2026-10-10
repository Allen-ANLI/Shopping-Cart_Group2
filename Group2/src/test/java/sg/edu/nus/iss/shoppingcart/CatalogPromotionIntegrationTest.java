package sg.edu.nus.iss.shoppingcart;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import sg.edu.nus.iss.shoppingcart.config.ReviewSampleInitializer;
import sg.edu.nus.iss.shoppingcart.dto.ProductForm;
import sg.edu.nus.iss.shoppingcart.entity.*;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.repository.*;
import sg.edu.nus.iss.shoppingcart.service.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Other legacy integration classes clear their shared H2 tables. This suite needs the
// real startup migrations and therefore owns its database instead of depending on test order.
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:catalog_promotions;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class CatalogPromotionIntegrationTest extends ModuleETestBase {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired OrderRepository orders;
    @Autowired OrderItemRepository items;
    @Autowired ProductReviewRepository reviews;
    @Autowired CatalogSeedVersionRepository versions;
    @Autowired AdminProductService admin;
    @Autowired ProductService catalog;
    @Autowired CartService cart;
    @Autowired CheckoutTransactionService checkout;
    @Autowired ReviewSampleInitializer samples;
    @Autowired EntityManager em;

    private ProductForm form(String name, String price, int discount) {
        var form = new ProductForm(); form.setName(name); form.setPrice(new BigDecimal(price));
        form.setCategory("charging"); form.setDiscountPercent(discount); form.setStockQuantity(100);
        return form;
    }
    private User user() {
        return users.saveAndFlush(new User("promo_" + UUID.randomUUID().toString().substring(0, 12), "hash", "Buyer", null));
    }
    private MockHttpSession session(User user) {
        var session = new MockHttpSession(); session.setAttribute("loginUserId", user.getId()); return session;
    }

    @Test void discountAppliesToCartCheckoutAndImmutableOrderSnapshots() throws Exception {
        ProductForm form = form("Discounted device " + UUID.randomUUID(), "99.99", 25);
        Product product = admin.create(form); User buyer = user(); var session = session(buyer);
        em.flush(); em.clear();
        cart.addItem(session, product.getId(), 3);
        var lines = cart.getCartItems(session);
        assertThat(lines.get(0).getUnitPrice()).isEqualByComparingTo("74.99");
        assertThat(cart.calculateTotal(lines)).isEqualByComparingTo("224.97");
        var order = checkout.createOrder(buyer.getId(), Map.of(product.getId(), 3), UUID.randomUUID().toString());
        em.flush(); em.clear();
        assertThat(orders.findById(order.getId()).orElseThrow().getTotalAmount()).isEqualByComparingTo("224.97");
        assertThat(items.findByOrder_IdOrderByIdAsc(order.getId()).get(0).getUnitPrice()).isEqualByComparingTo("74.99");
        form.setPrice(new BigDecimal("200.00")); form.setDiscountPercent(10); admin.update(product.getId(), form);
        em.flush(); em.clear();
        assertThat(products.findById(product.getId()).orElseThrow().getEffectivePrice()).isEqualByComparingTo("180.00");
        assertThat(items.findByOrder_IdOrderByIdAsc(order.getId()).get(0).getUnitPrice()).isEqualByComparingTo("74.99");
        mvc.perform(get("/api/products/{id}", product.getId())).andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(200.00)).andExpect(jsonPath("$.originalPrice").value(200.00))
                .andExpect(jsonPath("$.effectivePrice").value(180.00)).andExpect(jsonPath("$.discountPercent").value(10))
                .andExpect(jsonPath("$.onSale").value(true));
    }

    @Test void priceSortingUsesPayablePriceAndFullPriceProductsRemainUnchanged() {
        String marker = "Promotion sort " + UUID.randomUUID();
        Product discounted = admin.create(form(marker + " premium", "100.00", 75));
        Product regular = admin.create(form(marker + " regular", "40.00", 0));
        em.flush(); em.clear();
        assertThat(catalog.findProductPage(0, 10, null, marker, "price-asc").getContent())
                .extracting(Product::getId).containsExactly(discounted.getId(), regular.getId());
        assertThat(catalog.findProductPage(0, 10, null, marker, "price-desc").getContent())
                .extracting(Product::getId).containsExactly(regular.getId(), discounted.getId());
        assertThat(regular.getEffectivePrice()).isEqualByComparingTo(regular.getOriginalPrice());
        assertThat(regular.isOnSale()).isFalse();
        assertThatThrownBy(() -> catalog.findProductPage(0, 10, null, null, "name")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void discountValidationIsEnforcedAndDisplayedInSelectedLanguage() throws Exception {
        var form = form("Invalid promotion", "10.00", 100);
        assertThatThrownBy(() -> admin.create(form)).isInstanceOf(BusinessException.class);
        form.setDiscountPercent(-1);
        assertThatThrownBy(() -> admin.create(form)).isInstanceOf(BusinessException.class);
        User administrator = user(); administrator.setRole(User.Role.ADMIN); users.saveAndFlush(administrator);
        for (String language : List.of("en", "zh")) {
            mvc.perform(post("/admin/products").session(session(administrator))
                    .cookie(new jakarta.servlet.http.Cookie("store_lang", language))
                    .param("name", "Illegal promotion").param("price", "10.00").param("discountPercent", "100"))
                    .andExpect(status().isOk()).andExpect(model().attributeHasFieldErrors("productForm", "discountPercent"))
                    .andExpect(content().string(containsString(language.equals("en")
                            ? "Discount must be between 0 and 99 percent." : "折扣优惠必须为 0 至 99。")));
        }
        Product smallest = admin.create(form("Tiny price", "0.01", 99));
        assertThat(smallest.getEffectivePrice()).isEqualByComparingTo("0.01");
    }

    @Test void sampleMetricsHaveWideSpreadAndReviewsNeverExceedSales() {
        em.clear();
        List<Product> seeded = products.findAll().stream().filter(p -> p.getTotalReviews() >= 5).toList();
        assertThat(seeded).hasSizeGreaterThanOrEqualTo(66);
        assertThat(seeded).allSatisfy(p -> assertThat(p.getTotalReviews()).isLessThanOrEqualTo(p.getSalesCount()));
        assertThat(seeded).extracting(Product::getSalesCount).contains(5L, 20L, 65L, 180L, 550L, 1400L, 4200L, 9600L);
        assertThat(seeded).extracting(Product::getAverageRating).anyMatch(r -> r <= 2.0).anyMatch(r -> r == 5.0);
        assertThat(seeded.stream().filter(p -> p.getAverageRating() >= 4).count()).isGreaterThan(seeded.size() * 3L / 4);
        var countsBySales = new TreeMap<Long, Long>();
        seeded.forEach(p -> countsBySales.put(p.getSalesCount(), p.getTotalReviews()));
        assertThat(new ArrayList<>(countsBySales.values())).isSorted();
        assertThat(seeded).anyMatch(Product::isOnSale).anyMatch(p -> !p.isOnSale());
    }

    @Test void sampleMigrationPreservesCustomerPurchasesAndReviewsAndIsIdempotent() {
        Product product = admin.create(form("Customer purchase to preserve", "50.00", 20));
        User customer = user();
        var order = new Order(customer, product.getEffectivePrice(), UUID.randomUUID().toString());
        order.recordPayment("VISA", "customer-payment"); order.confirmReceipt(); orders.saveAndFlush(order);
        items.saveAndFlush(new OrderItem(order, product, 1));
        ProductReview review = new ProductReview(); review.setUser(customer); review.setProduct(product);
        review.setRating(4); review.setComment("Keep my customer review"); review.setCreatedAt(LocalDateTime.now());
        reviews.saveAndFlush(review); Long reviewId = review.getId(); Long orderId = order.getId();
        versions.deleteById(ReviewSampleInitializer.VERSION); versions.flush();
        samples.run(); em.flush(); em.clear();
        assertThat(orders.findById(orderId)).isPresent();
        assertThat(reviews.findById(reviewId).orElseThrow().getComment()).isEqualTo("Keep my customer review");
        assertThat(products.findById(product.getId()).orElseThrow().getSalesCount()).isEqualTo(1);
        long reviewCount = reviews.count(), orderCount = orders.count();
        samples.run();
        assertThat(reviews.count()).isEqualTo(reviewCount); assertThat(orders.count()).isEqualTo(orderCount);
    }
}
