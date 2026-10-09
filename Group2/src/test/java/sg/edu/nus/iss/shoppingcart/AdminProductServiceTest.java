package sg.edu.nus.iss.shoppingcart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import sg.edu.nus.iss.shoppingcart.entity.Order;
import sg.edu.nus.iss.shoppingcart.entity.OrderItem;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.exception.ResourceNotFoundException;
import sg.edu.nus.iss.shoppingcart.repository.OrderItemQueryRepository;
import sg.edu.nus.iss.shoppingcart.repository.OrderQueryRepository;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;
import sg.edu.nus.iss.shoppingcart.service.AdminProductService;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 管理员商品管理服务测试 —— E 模块。
 *
 * <p>逐条验证分工文档给 E 的验收标准：</p>
 * <ul>
 *   <li>"名称与价格等非法输入被后端拒绝"</li>
 *   <li>"优先下架，不物理删除被订单引用的商品"</li>
 *   <li>"下架商品不再出售，但已有历史订单仍完整显示"</li>
 * </ul>
 *
 * @author 蔡千一（Module E）
 */
class AdminProductServiceTest extends ModuleETestBase {

    @Autowired
    private AdminProductService adminProductService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderQueryRepository orderRepository;

    @Autowired
    private OrderItemQueryRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    // ---------- 校验 ----------

    @Test
    @DisplayName("新增商品时空白名称被拒绝")
    void rejectsBlankName() {
        assertThatThrownBy(() ->
                adminProductService.create("   ", "desc", new BigDecimal("10.00"), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("name is required");

        assertThatThrownBy(() ->
                adminProductService.create(null, "desc", new BigDecimal("10.00"), null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("新增商品时非正数价格被拒绝")
    void rejectsNonPositivePrice() {
        assertThatThrownBy(() ->
                adminProductService.create("Item", null, BigDecimal.ZERO, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("greater than 0");

        assertThatThrownBy(() ->
                adminProductService.create("Item", null, new BigDecimal("-5.00"), null))
                .isInstanceOf(BusinessException.class);

        assertThatThrownBy(() ->
                adminProductService.create("Item", null, null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("价格小数位超过两位被拒绝")
    void rejectsTooManyDecimalPlaces() {
        assertThatThrownBy(() ->
                adminProductService.create("Item", null, new BigDecimal("10.999"), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2 decimal places");
    }

    @Test
    @DisplayName("超长名称被拒绝")
    void rejectsOverlongName() {
        String tooLong = "x".repeat(101);

        assertThatThrownBy(() ->
                adminProductService.create(tooLong, null, new BigDecimal("10.00"), null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("100 characters");
    }

    @Test
    @DisplayName("编辑不存在的商品抛出未找到异常")
    void rejectsEditingMissingProduct() {
        assertThatThrownBy(() ->
                adminProductService.update(999999L, "Name", null, new BigDecimal("1.00"), null, true))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- 下架 vs 删除 ----------

    @Test
    @DisplayName("从未被下单的商品可以物理删除")
    void canDeleteUnreferencedProduct() {
        Product product = adminProductService.create(
                "Mistake", null, new BigDecimal("10.00"), null);

        adminProductService.deleteIfUnreferenced(product.getId());

        assertThat(productRepository.findById(product.getId())).isEmpty();
    }

    @Test
    @DisplayName("被订单引用的商品不能物理删除，必须改为下架")
    void cannotDeleteProductReferencedByOrder() {
        // 这是分工文档的硬性要求：优先下架，不物理删除被订单引用的商品。
        // 明细表上有指向 products 的外键，删掉商品会让历史订单保存失败。
        User user = createUser("buyer");
        Product product = adminProductService.create(
                "Popular", null, new BigDecimal("25.00"), null);
        createOrder(user, product);

        assertThatThrownBy(() -> adminProductService.deleteIfUnreferenced(product.getId()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cannot be deleted");

        // 商品还在，可以改走下架这条路
        assertThat(productRepository.findById(product.getId())).isPresent();

        adminProductService.toggleActive(product.getId());
        assertThat(productRepository.findById(product.getId()).orElseThrow().isActive())
                .isFalse();
    }

    @Test
    @DisplayName("下架商品后，历史订单依然能查到且明细完整")
    void hidingProductKeepsHistoricalOrdersIntact() {
        User user = createUser("buyer2");
        Product product = adminProductService.create(
                "To Hide", null, new BigDecimal("30.00"), null);
        Order order = createOrder(user, product);

        adminProductService.toggleActive(product.getId());

        // 商品确实下架了
        assertThat(productRepository.findById(product.getId()).orElseThrow().isActive()).isFalse();

        // 但明细还在，快照完好
        var items = orderItemRepository.findByOrder_IdOrderByIdAsc(order.getId());
        assertThat(items).hasSize(1);
        assertThat(items.get(0).getProductNameSnapshot()).isEqualTo("To Hide");
        assertThat(items.get(0).getUnitPrice()).isEqualByComparingTo("30.00");
    }

    // ---------- 上下架 ----------

    @Test
    @DisplayName("上下架切换可以反复进行")
    void togglesActiveState() {
        Product product = adminProductService.create(
                "Toggle", null, new BigDecimal("5.00"), null);

        assertThat(product.isActive()).isTrue();

        adminProductService.toggleActive(product.getId());
        assertThat(productRepository.findById(product.getId()).orElseThrow().isActive()).isFalse();

        adminProductService.toggleActive(product.getId());
        assertThat(productRepository.findById(product.getId()).orElseThrow().isActive()).isTrue();
    }

    // ---------- 编辑 ----------

    @Test
    @DisplayName("编辑商品会保存名称、描述、价格与图片地址")
    void updatesProductFields() {
        Product product = adminProductService.create(
                "Before", "old", new BigDecimal("10.00"), null);

        adminProductService.update(product.getId(), "After", "new",
                new BigDecimal("20.00"), "/images/new.png", false);

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo("After");
        assertThat(reloaded.getDescription()).isEqualTo("new");
        assertThat(reloaded.getPrice()).isEqualByComparingTo("20.00");
        assertThat(reloaded.getImageUrl()).isEqualTo("/images/new.png");
        assertThat(reloaded.isActive()).isFalse();
    }

    @Test
    @DisplayName("空白字符串被归一化为 null，不在库里存空串")
    void normalisesBlankStringsToNull() {
        Product product = adminProductService.create(
                "Item", "   ", new BigDecimal("10.00"), "  ");

        Product reloaded = productRepository.findById(product.getId()).orElseThrow();
        assertThat(reloaded.getDescription()).isNull();
        assertThat(reloaded.getImageUrl()).isNull();
    }

    @Test
    @DisplayName("后台列表包含已下架商品")
    void adminListIncludesHiddenProducts() {
        Product visible = adminProductService.create("Visible", null, new BigDecimal("1.00"), null);
        Product hidden = adminProductService.create("Hidden", null, new BigDecimal("2.00"), null);
        adminProductService.toggleActive(hidden.getId());

        var all = adminProductService.findAllForAdmin(0).getContent();

        // 顾客侧看不到已下架商品，后台必须看得到，否则管理员没法重新上架
        assertThat(all).extracting(Product::getId)
                .contains(visible.getId(), hidden.getId());
        assertThat(adminProductService.countInactive()).isEqualTo(1);
    }

    // ---------- 测试夹具 ----------

    private User createUser(String username) {
        User user = new User(username, "pw", username, username + "@test.local");
        return userRepository.save(user);
    }

    private Order createOrder(User user, Product product) {
        Order order = new Order(user, product.getPrice(), UUID.randomUUID().toString());
        orderRepository.save(order);
        orderItemRepository.save(new OrderItem(order, product, 1));
        return order;
    }
}
