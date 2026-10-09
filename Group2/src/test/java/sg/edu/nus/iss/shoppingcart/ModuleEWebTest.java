package sg.edu.nus.iss.shoppingcart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import sg.edu.nus.iss.shoppingcart.entity.Order;
import sg.edu.nus.iss.shoppingcart.entity.OrderItem;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.interceptor.CurrentUser;
import sg.edu.nus.iss.shoppingcart.repository.OrderItemQueryRepository;
import sg.edu.nus.iss.shoppingcart.repository.OrderQueryRepository;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * 页面与权限测试 —— E 模块。
 *
 * <p>为什么需要这一层：Service 测试能证明业务逻辑对，但<b>证明不了
 * Thymeleaf 模板能渲染</b>。模板里写错属性名、引用了不存在的变量，
 * 只有真正渲染一次才会暴露。同理，拦截器的重定向行为也只能在
 * MockMvc 的请求上下���里验证。</p>
 *
 * <p>登录身份通过往 MockHttpSession 里写 {@code loginUserId} 模拟——
 * 这正是 B 的登录流程做的事。这样测试不需要真的走登录表单，
 * 也不需要在项目里留任何后门接口。</p>
 *
 * @author蔡千一（Module E）
 * @author OpenAI Codex (new product visibility regression)
 */
@AutoConfigureMockMvc
class ModuleEWebTest extends ModuleETestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderQueryRepository orderRepository;

    @Autowired
    private OrderItemQueryRepository orderItemRepository;

    /** 普通用户会话。 */
    private MockHttpSession customerSession;

    /** 管理员会话。 */
    private MockHttpSession adminSession;

    /** 测试用普通用户。 */
    private User customer;

    /** 测试用管理员。 */
    private User admin;

    @BeforeEach
    void setUp() {
        customer = createUser("web_customer", User.Role.CUSTOMER);
        admin = createUser("web_admin", User.Role.ADMIN);

        customerSession = sessionFor(customer.getId());
        adminSession = sessionFor(admin.getId());
    }

    // ---------- 权限拦截 ----------

    @Test
    @DisplayName("未登录访问后台被重定向到登录页")
    void anonymousCannotReachAdminPanel() throws Exception {
        mockMvc.perform(get("/admin/products"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("普通用户访问后台被拒绝（不是跳登录，是明确拒绝）")
    void normalUserCannotReachAdminPanel() throws Exception {
        // 分工文档验收标准：普通用户直接输入后台 URL 也被拒绝。
        // 这里断言它不是重定向到 /login——那样会让人误以为自己没登录。
        MvcResult result = mockMvc.perform(get("/admin/products")
                        .session(customerSession))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(result.getResponse().getRedirectedUrl())
                .isNotNull()
                .doesNotContain("/login")
                .contains("/forbidden");
    }

    @Test
    @DisplayName("普通用户直接 POST 后台表单同样被拦，不是只隐藏入口")
    void normalUserCannotPostToAdminPanel() throws Exception {
        // 分工文档明确：页面隐藏按钮不能代替后端权限检查。
        mockMvc.perform(post("/admin/products")
                        .session(customerSession)
                        .param("name", "Injected")
                        .param("price", "1.00"))
                .andExpect(status().is3xxRedirection());

        // 关键：不能真的写进库
        assertThat(productRepository.findAll())
                .noneMatch(p -> "Injected".equals(p.getName()));
    }

    @Test
    @DisplayName("管理员可以访问后台")
    void adminCanReachAdminPanel() throws Exception {
        mockMvc.perform(get("/admin/products").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/products"));
    }

    // ---------- 模板渲染 ----------

    @Test
    @DisplayName("新增商品时取消上架勾选，商品保存但不向顾客出售")
    void newProductHonorsUncheckedVisibility() throws Exception {
        mockMvc.perform(post("/admin/products").session(adminSession)
                        .param("name", "Hidden new product").param("price", "25.00")
                        .param("_active", "on"))
                .andExpect(status().is3xxRedirection());
        Product saved = productRepository.findAll().stream()
                .filter(p -> "Hidden new product".equals(p.getName())).findFirst().orElseThrow();
        assertThat(saved.isActive()).isFalse();
        mockMvc.perform(get("/api/products/" + saved.getId())).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("订单历史页成功渲染，含分页与统计")
    void orderHistoryRenders() throws Exception {
        Product product = createProduct("Ramp Item", "20.00");
        for (int i = 0; i < 6; i++) {
            createOrder(customer, product);
        }

        mockMvc.perform(get("/orders").session(customerSession))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/history"))
                .andExpect(model().attributeExists("orders", "orderCount", "lifetimeSpend"))
                .andExpect(content().string(containsString("Lifetime spend")))
                // 6 条订单、每页 5 条 -> 必须出现分页控件
                .andExpect(content().string(containsString("pagination")));
    }

    @Test
    @DisplayName("订单历史页翻页可用，且两页内容不重复")
    void orderHistoryPaginationWorks() throws Exception {
        Product product = createProduct("Paged Item", "15.00");
        for (int i = 0; i < 7; i++) {
            createOrder(customer, product);
        }

        MvcResult page1 = mockMvc.perform(get("/orders").param("page", "1")
                        .session(customerSession))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult page2 = mockMvc.perform(get("/orders").param("page", "2")
                        .session(customerSession))
                .andExpect(status().isOk())
                .andReturn();

        String html1 = page1.getResponse().getContentAsString();
        String html2 = page2.getResponse().getContentAsString();

        // 第 1 页 5 条订单，第 2 页 2 条
        assertThat(countOccurrences(html1, "View details")).isEqualTo(5);
        assertThat(countOccurrences(html2, "View details")).isEqualTo(2);
    }

    @Test
    @DisplayName("非法页码不会 500，正常返回")
    void orderHistoryToleratesInvalidPage() throws Exception {
        mockMvc.perform(get("/orders").param("page", "-5").session(customerSession))
                .andExpect(status().isOk());

        mockMvc.perform(get("/orders").param("page", "0").session(customerSession))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("订单详情页成功渲染，显示成交快照单价")
    void orderDetailRendersSnapshotPrice() throws Exception {
        Product product = createProduct("Snapshot Widget", "42.00");
        Order order = createOrder(customer, product);

        // 下单后立刻改价，验证详情页显示的还是 42.00
        product.setPrice(new BigDecimal("99.00"));
        productRepository.save(product);

        mockMvc.perform(get("/orders/" + order.getId()).session(customerSession))
                .andExpect(status().isOk())
                .andExpect(view().name("orders/detail"))
                .andExpect(content().string(containsString("42.00")))
                .andExpect(content().string(
                        not(containsString("99.00"))))
                .andExpect(content().string(containsString("Snapshot Widget")));
    }

    @Test
    @DisplayName("越权访问他人订单返回 404，页面提示与不存在时一致")
    void crossUserOrderAccessReturns404() throws Exception {
        Product product = createProduct("Private Item", "30.00");
        Order victimOrder = createOrder(customer, product);

        // attacker 是一个合法登录的另一个用户
        User attacker = createUser("web_attacker", User.Role.CUSTOMER);
        MockHttpSession attackerSession = sessionFor(attacker.getId());

        String realResponse = mockMvc.perform(
                        get("/orders/" + victimOrder.getId()).session(attackerSession))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        String fakeResponse = mockMvc.perform(
                        get("/orders/999999").session(attackerSession))
                .andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();

        // 两句提示必须一致，否则能探测出订单是否存在
        assertThat(extractMessage(realResponse))
                .isEqualTo(extractMessage(fakeResponse))
                .isNotBlank();
    }

    @Test
    @DisplayName("非法订单 ID 返回 400 而不是 500")
    void invalidOrderIdReturns400() throws Exception {
        mockMvc.perform(get("/orders/not-a-number").session(customerSession))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("后台新增表单渲染，含图片地址字段")
    void adminCreateFormRenders() throws Exception {
        mockMvc.perform(get("/admin/products/new").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(content().string(containsString("imageUrl")))
                .andExpect(content().string(containsString("price")));
    }

    @Test
    @DisplayName("后台编辑表单回填商品当前值")
    void adminEditFormPrefillsValues() throws Exception {
        Product product = createProduct("Edit Me", "77.00");

        mockMvc.perform(get("/admin/products/" + product.getId() + "/edit")
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(content().string(containsString("Edit Me")))
                .andExpect(content().string(containsString("77.00")));
    }

    // ---------- 服务端校验（MockMvc 提交真实表单） ----------

    @Test
    @DisplayName("后台提交空白名称：回显错误且不写库")
    void rejectsBlankNameViaForm() throws Exception {
        mockMvc.perform(post("/admin/products")
                        .session(adminSession)
                        .param("name", "   ")
                        .param("price", "10.00"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(content().string(
                        containsString("Product name is required")));

        assertThat(productRepository.findAll()).noneMatch(p -> "   ".equals(p.getName()));
    }

    @Test
    @DisplayName("后台提交负数价格：回显错误且不写库")
    void rejectsNegativePriceViaForm() throws Exception {
        mockMvc.perform(post("/admin/products")
                        .session(adminSession)
                        .param("name", "Negative")
                        .param("price", "-5"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(content().string(
                        containsString("greater than 0")));

        assertThat(productRepository.findAll()).noneMatch(p -> "Negative".equals(p.getName()));
    }

    @Test
    @DisplayName("后台合法提交：写库并重定向")
    void acceptsValidProductViaForm() throws Exception {
        mockMvc.perform(post("/admin/products")
                        .session(adminSession)
                        .param("name", "Valid Item")
                        .param("description", "desc")
                        .param("price", "12.50")
                        .param("imageUrl", "/images/x.png"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products?created"));

        assertThat(productRepository.findAll())
                .anySatisfy(p -> {
                    assertThat(p.getName()).isEqualTo("Valid Item");
                    assertThat(p.getPrice()).isEqualByComparingTo("12.50");
                    assertThat(p.getImageUrl()).isEqualTo("/images/x.png");
                    assertThat(p.isActive()).isTrue();
                });
    }

    @Test
    @DisplayName("后台删除被引用商品：提示改用下架，不删也不报错")
    void deletingReferencedProductShowsGuidance() throws Exception {
        Product product = createProduct("Sold Out", "25.00");
        createOrder(customer, product);

        mockMvc.perform(post("/admin/products/" + product.getId() + "/delete")
                        .session(adminSession))
                .andExpect(status().isOk())
                .andExpect(view().name("error/business"))
                .andExpect(content().string(
                        containsString("cannot be deleted")));

        // 商品还在
        assertThat(productRepository.findById(product.getId())).isPresent();
    }

    @Test
    @DisplayName("后台删除未被引用商品：成功并重定向")
    void deletesUnreferencedProduct() throws Exception {
        Product product = createProduct("Unsold", "25.00");

        mockMvc.perform(post("/admin/products/" + product.getId() + "/delete")
                        .session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/products?deleted"));

        assertThat(productRepository.findById(product.getId())).isEmpty();
    }

    @Test
    @DisplayName("后台下架商品：写库并重定向")
    void togglesProductVisibility() throws Exception {
        Product product = createProduct("Toggleable", "25.00");
        assertThat(product.isActive()).isTrue();

        mockMvc.perform(post("/admin/products/" + product.getId() + "/toggle")
                        .session(adminSession))
                .andExpect(status().is3xxRedirection());

        assertThat(productRepository.findById(product.getId()).orElseThrow().isActive())
                .isFalse();
    }

    @Test
    @DisplayName("编辑不存在的商品返回 404")
    void editingMissingProductReturns404() throws Exception {
        mockMvc.perform(get("/admin/products/999999/edit").session(adminSession))
                .andExpect(status().isNotFound());
    }

    // ---------- 测试夹具 ----------

    /** 造一个带指定身份的 Session，模拟登录流程写入 loginUserId。 */
    private MockHttpSession sessionFor(Long userId) {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute(CurrentUser.LOGIN_USER_ID, userId);
        return session;
    }

    private User createUser(String username, User.Role role) {
        User user = new User(username, "pw", username, username + "@test.local");
        user.setRole(role);
        return userRepository.save(user);
    }

    private Product createProduct(String name, String price) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setActive(true);
        return productRepository.save(product);
    }

    private Order createOrder(User user, Product product) {
        Order order = new Order(user, product.getPrice(), UUID.randomUUID().toString());
        orderRepository.save(order);
        orderItemRepository.save(new OrderItem(order, product, 1));
        return order;
    }

    private int countOccurrences(String haystack, String needle) {
        int count = 0;
        int idx = 0;
        while ((idx = haystack.indexOf(needle, idx)) >= 0) {
            count++;
            idx += needle.length();
        }
        return count;
    }

    /** 从错误页里抠出提示文案，用来比较越权与不存在的提示是否一致。 */
    private String extractMessage(String html) {
        var matcher = java.util.regex.Pattern
                .compile("<p class=\"muted\">([^<]*)</p>")
                .matcher(html);
        return matcher.find() ? matcher.group(1).trim() : "";
    }
}
