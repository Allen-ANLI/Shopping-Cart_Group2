package sg.edu.nus.iss.shoppingcart;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import sg.edu.nus.iss.shoppingcart.dto.ProductForm;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.interceptor.CurrentUser;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import sg.edu.nus.iss.shoppingcart.repository.ProductReviewRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;
import sg.edu.nus.iss.shoppingcart.service.AdminProductService;
import sg.edu.nus.iss.shoppingcart.service.CartService;
import sg.edu.nus.iss.shoppingcart.service.ProductService;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class CatalogReviewIntegrationTest extends ModuleETestBase {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired ProductReviewRepository reviews;
    @Autowired UserRepository users;
    @Autowired ProductService catalog;
    @Autowired CartService cart;
    @Autowired AdminProductService admin;
    @Autowired EntityManager entityManager;

    @Test void filtersSearchesAndSortsOnlyActiveProductsAndRetainsBilingualMetadata() throws Exception {
        String marker = UUID.randomUUID().toString().substring(0, 8);
        Product expensive = product(marker + " Desk Pro", "workspace", "60.00", true);
        Product cheap = product(marker + " Desk Mini", "workspace", "15.00", true);
        product(marker + " Hidden", "workspace", "1.00", false);
        product(marker + " Audio", "audio", "2.00", true);
        mvc.perform(get("/api/products").param("page", "0").param("size", "12")
                        .param("category", "workspace").param("q", marker).param("sort", "price-asc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].id").value(cheap.getId()))
                .andExpect(jsonPath("$.content[1].id").value(expensive.getId()))
                .andExpect(jsonPath("$.content[0].category").value("workspace"))
                .andExpect(jsonPath("$.content[0].brand").value("Test Studio"))
                .andExpect(jsonPath("$.content[0].nameZh").value("测试商品"));
        mvc.perform(get("/api/products").param("page", "0").param("category", "workspace")
                        .param("q", marker).param("sort", "price-desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].id").value(expensive.getId()));
        assertThat(catalog.findProductPage(0, 10, "workspace", marker, "featured").getTotalElements()).isEqualTo(2);
        // Existing clients without a page parameter still receive a list.
        mvc.perform(get("/api/products").param("q", marker).param("category", "audio"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/categories")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].slug", contains("computing", "typing", "workspace", "audio")))
                .andExpect(jsonPath("$[0].nameZh").value("电脑与配件"));
    }

    @Test void queryEscapesSqlWildcardsAndRejectsUnknownCategoryAndSort() throws Exception {
        String marker = UUID.randomUUID().toString();
        product(marker + "% literal", "typing", "8.00", true);
        product(marker + " other", "typing", "9.00", true);
        assertThat(catalog.findProductPage(0, 10, null, marker + "%", "featured").getTotalElements()).isEqualTo(1);
        mvc.perform(get("/api/products").param("page", "0").param("category", "unknown"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        mvc.perform(get("/api/products").param("page", "0").param("sort", "passwordHash"))
                .andExpect(status().isBadRequest());
    }

    @Test void anonymousCanReadReviewsButCannotWriteOrImpersonateAUser() throws Exception {
        Product product = product("Anonymous review target", "audio", "10.00", true);
        mvc.perform(get("/api/products/{id}/reviews", product.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalReviews").value(0))
                .andExpect(jsonPath("$.canReview").value(false));
        mvc.perform(post("/api/products/{id}/reviews", product.getId()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"Lovely product\",\"userId\":1}"))
                .andExpect(status().isUnauthorized());
        assertThat(reviews.findByProductIdOrderByCreatedAtDescIdDesc(product.getId())).isEmpty();
    }

    @Test void writesReviewsForSessionOwnerPersistsAndUpdatesInsteadOfDuplicating() throws Exception {
        User user = user("Reviewer");
        User other = user("Other user");
        Product product = product("Review target", "audio", "10.00", true);
        MockHttpSession session = session(user);
        String token = cart.formToken(session);
        mvc.perform(post("/api/products/{id}/reviews", product.getId()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4,\"comment\":\"Very clear sound\","
                                + "\"cartFormToken\":\"" + token + "\",\"userId\":" + other.getId() + ",\"displayName\":\"Spoofed\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalReviews").value(1))
                .andExpect(jsonPath("$.reviews[0].displayName").value("Reviewer"))
                .andExpect(jsonPath("$.reviews[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.reviews[0].email").doesNotExist())
                .andExpect(jsonPath("$.ownReview.rating").value(4));
        entityManager.clear();
        var saved = reviews.findByProductIdAndUserId(product.getId(), user.getId()).orElseThrow();
        Long originalId = saved.getId();
        assertThat(saved.getComment()).isEqualTo("Very clear sound");
        assertThat(reviews.findByProductIdAndUserId(product.getId(), other.getId())).isEmpty();
        MockHttpSession nextLogin = session(user);
        mvc.perform(get("/api/products/{id}/reviews", product.getId()).session(nextLogin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.ownReview.comment").value("Very clear sound"));
        String newToken = cart.formToken(nextLogin);
        mvc.perform(post("/api/products/{id}/reviews", product.getId()).session(nextLogin)
                        .contentType(MediaType.APPLICATION_JSON).content(body(5, "Updated after a week", newToken)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalReviews").value(1))
                .andExpect(jsonPath("$.averageRating").value(5.0));
        entityManager.clear();
        assertThat(reviews.findByProductIdAndUserId(product.getId(), user.getId()).orElseThrow().getId()).isEqualTo(originalId);
        assertThatThrownBy(() -> admin.deleteIfUnreferenced(product.getId())).isInstanceOf(BusinessException.class);
    }

    @Test void rejectsExpiredTokenInvalidRatingsWhitespaceAndHiddenProducts() throws Exception {
        User user = user("Valid user");
        MockHttpSession session = session(user);
        String token = cart.formToken(session);
        Product product = product("Validation target", "computing", "10.00", true);
        mvc.perform(post("/api/products/{id}/reviews", product.getId()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body(5, "Normal comment", "wrong")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/products/{id}/reviews", product.getId()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body(6, "Normal comment", token)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/products/{id}/reviews", product.getId()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body(5, "        ", token)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/products/{id}/reviews", product.getId()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body(5, "  abc  ", token)))
                .andExpect(status().isBadRequest());
        product.setActive(false); products.saveAndFlush(product);
        mvc.perform(post("/api/products/{id}/reviews", product.getId()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(body(5, "Normal comment", token)))
                .andExpect(status().isNotFound());
        assertThat(reviews.findByProductIdOrderByCreatedAtDescIdDesc(product.getId())).isEmpty();
    }

    @Test void administratorCatalogMetadataPersistsAndRendersInBothLanguages() throws Exception {
        ProductForm form = new ProductForm();
        form.setName("New monitor"); form.setNameZh("新显示器"); form.setPrice(new BigDecimal("123.00"));
        form.setCategory("computing"); form.setBrand("Test Brand");
        form.setOrigin("Singapore"); form.setOriginZh("新加坡"); form.setDescriptionZh("清晰易用的显示器。");
        Long id = admin.create(form).getId();
        entityManager.flush(); entityManager.clear();
        Product stored = products.findById(id).orElseThrow();
        assertThat(stored.getCategory()).isEqualTo("computing");
        assertThat(stored.getBrand()).isEqualTo("Test Brand");
        assertThat(stored.getNameZh()).isEqualTo("新显示器");
        User administrator = user("Administrator"); administrator.setRole(User.Role.ADMIN); users.saveAndFlush(administrator);
        mvc.perform(get("/admin/products/{id}/edit", id).session(session(administrator)).cookie(new jakarta.servlet.http.Cookie("store_lang", "zh")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("商品名称（中文）")))
                .andExpect(content().string(containsString("新显示器")));
        mvc.perform(get("/admin/products").session(session(administrator)).cookie(new jakarta.servlet.http.Cookie("store_lang", "en")))
                .andExpect(status().isOk()).andExpect(content().string(containsString("Test Brand")));
    }

    private Product product(String name, String category, String price, boolean active) {
        Product product = new Product(); product.setName(name); product.setNameZh("测试商品");
        product.setCategory(category); product.setBrand("Test Studio"); product.setPrice(new BigDecimal(price));
        product.setDescription("A test product"); product.setOrigin("Singapore"); product.setActive(active);
        return products.saveAndFlush(product);
    }
    private User user(String displayName) {
        return users.saveAndFlush(new User("r" + UUID.randomUUID().toString().substring(0, 12), "hash", displayName, null));
    }
    private MockHttpSession session(User user) {
        MockHttpSession session = new MockHttpSession(); session.setAttribute(CurrentUser.LOGIN_USER_ID, user.getId()); return session;
    }
    private String body(int rating, String comment, String token) {
        return "{\"rating\":" + rating + ",\"comment\":\"" + comment + "\",\"cartFormToken\":\"" + token + "\"}";
    }
}
