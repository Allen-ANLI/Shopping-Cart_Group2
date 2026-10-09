package sg.edu.nus.iss.shoppingcart;

import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;
import sg.edu.nus.iss.shoppingcart.service.CartService;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/** 验证搜索、稳定排序、购物车计数及清空前确认的实际请求流程。
 * @author 王重一
 */
@AutoConfigureMockMvc
class CatalogBrowsingTest extends ModuleETestBase {
    @Autowired MockMvc mvc;
    @Autowired ProductRepository products;
    @Autowired UserRepository users;
    @Autowired CartService cart;
    Product cheap, expensive;
    MockHttpSession session;

    @BeforeEach void prepare() {
        cheap = product("UXNeedle Mouse", "5.00", true);
        expensive = product("UXNeedle Keyboard", "70.00", true);
        product("UXNeedle Hidden", "1.00", false);
        User user = new User(); user.setUsername("ux-browser-test");
        user.setDisplayName("UI Tester"); user.setPasswordHash("test-only-hash");
        users.saveAndFlush(user);
        session = new MockHttpSession(); session.setAttribute("loginUserId", user.getId());
    }
    Product product(String name, String price, boolean active) {
        Product product = new Product(); product.setName(name);
        product.setPrice(new BigDecimal(price)); product.setActive(active);
        return products.saveAndFlush(product);
    }
    @Test void searchCoversAllPagesAndExcludesInactiveProducts() throws Exception {
        mvc.perform(get("/api/products").param("page", "0").param("size", "1")
                .param("q", "  uxneedle  ").param("sort", "price-desc"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
            .andExpect(jsonPath("$.content[0].id").value(expensive.getId()));
        mvc.perform(get("/api/products").param("page", "1").param("size", "1")
                .param("q", "UXNEEDLE").param("sort", "price-desc"))
            .andExpect(jsonPath("$.content[0].id").value(cheap.getId()));
    }
    @Test void equalPricesHaveStableIdOrdering() throws Exception {
        Product tie = product("UXNeedle Second Mouse", "5.00", true);
        mvc.perform(get("/api/products").param("page", "0").param("size", "6")
                .param("q", "uxneedle").param("sort", "price-asc"))
            .andExpect(jsonPath("$.content[0].id").value(cheap.getId()))
            .andExpect(jsonPath("$.content[1].id").value(tie.getId()))
            .andExpect(jsonPath("$.content[2].id").value(expensive.getId()));
    }
    @Test void searchTreatsLikeWildcardsAsLiteralText() throws Exception {
        Product literal = product("UXNeedle %_ literal", "10.00", true);
        mvc.perform(get("/api/products").param("page", "0").param("q", "%_"))
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(literal.getId()));
    }
    @Test void unmatchedSearchIsEmpty() throws Exception {
        mvc.perform(get("/api/products").param("page", "0").param("q", "no-ux-such-product"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
    }
    @Test void invalidSortAndOversizedSearchAreBadRequests() throws Exception {
        mvc.perform(get("/api/products").param("page", "0").param("sort", "unknown"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/products").param("page", "0").param("q", "x".repeat(101)))
            .andExpect(status().isBadRequest());
    }
    @Test void identityReturnsTheCurrentSessionQuantity() throws Exception {
        cart.addItem(session, cheap.getId(), 3);
        mvc.perform(get("/api/auth/session").session(session))
            .andExpect(jsonPath("$.cartQuantity").value(3));
        mvc.perform(get("/api/auth/session"))
            .andExpect(jsonPath("$.cartQuantity").value(0));
    }
    @Test void clearConfirmationDoesNotClearUntilPosted() throws Exception {
        cart.addItem(session, cheap.getId(), 2);
        mvc.perform(get("/cart/clear").session(session)).andExpect(status().isOk())
            .andExpect(view().name("cart/clear"))
            .andExpect(content().string(containsString("Cancel")))
            .andExpect(model().attribute("totalQuantity", 2));
        mvc.perform(get("/api/auth/session").session(session))
            .andExpect(jsonPath("$.cartQuantity").value(2));
        mvc.perform(post("/cart/clear").session(session)
                .param("cartFormToken", cart.formToken(session)))
            .andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/auth/session").session(session))
            .andExpect(jsonPath("$.cartQuantity").value(0));
    }
    @Test void errorPageKeepsTheAuthenticatedNavigation() throws Exception {
        mvc.perform(get("/orders/999999999").session(session))
            .andExpect(status().isNotFound())
            .andExpect(content().string(containsString("Log out")))
            .andExpect(content().string(not(containsString(">Log in<"))));
    }
    @Test void invalidPageKeepsAuthenticatedNavigationAndApisKeepJsonErrors() throws Exception {
        mvc.perform(get("/orders/abc").session(session)).andExpect(status().isBadRequest())
            .andExpect(content().string(containsString("Log out")));
        mvc.perform(get("/api/products/abc").accept("application/json"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
    }

}
