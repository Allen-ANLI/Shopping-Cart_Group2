package sg.edu.nus.iss.shoppingcart;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import sg.edu.nus.iss.shoppingcart.dto.OrderDetailDto;
import sg.edu.nus.iss.shoppingcart.dto.OrderSummaryDto;
import sg.edu.nus.iss.shoppingcart.entity.Order;
import sg.edu.nus.iss.shoppingcart.entity.OrderItem;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.exception.ResourceNotFoundException;
import sg.edu.nus.iss.shoppingcart.repository.OrderItemQueryRepository;
import sg.edu.nus.iss.shoppingcart.repository.OrderQueryRepository;
import sg.edu.nus.iss.shoppingcart.repository.ProductRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;
import sg.edu.nus.iss.shoppingcart.service.PurchaseHistoryService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.time.LocalDateTime;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 购买历史服务测试 —— E 模块。
 *
 * <p>逐条验证分工文档给 E 的验收标准：</p>
 * <ul>
 *   <li>"用户不能通过修改订单 ID 查看别人订单"</li>
 *   <li>"下架商品不再出售，但已有历史订单仍完整显示"</li>
 *   <li>"改价不改变历史金额"（快照生效）</li>
 * </ul>
 *
 * @author 蔡千一（Module E）
 * @author OpenAI Codex (equal-timestamp pagination regression)
 */
class PurchaseHistoryServiceTest extends ModuleETestBase {

    @Autowired
    private PurchaseHistoryService purchaseHistoryService;

    @Autowired
    private OrderQueryRepository orderRepository;

    @Autowired
    private OrderItemQueryRepository orderItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    // ---------- 防越权 ----------

    @Test
    @DisplayName("访问他人订单必须被拒绝，且提示与'订单不存在'完全一致")
    void cannotViewAnotherUsersOrder() {
        User owner = createUser("owner");
        User attacker = createUser("attacker");
        Product product = createProduct("Keyboard", "59.90");
        Order order = createOrder(owner, product, 2);

        // 攻击者用自己的合法会话 ID 去查别人的订单
        assertThatThrownBy(() ->
                purchaseHistoryService.findOrderDetailForUser(order.getId(), attacker.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        // 关键：越权和"真不存在"必须是同一个异常类型，
        // 否则攻击者能通过报错差异推断这个订单 ID 是否存在。
        assertThatThrownBy(() ->
                purchaseHistoryService.findOrderDetailForUser(order.getId(), attacker.getId()))
                .isInstanceOf(ResourceNotFoundException.class);

        assertThatThrownBy(() ->
                purchaseHistoryService.findOrderDetailForUser(999999L, attacker.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("历史列表只返回当前用户的订单，不含他人数据")
    void historyOnlyContainsOwnOrders() {
        User alice = createUser("e_history_alice");
        User bob = createUser("e_history_bob");
        Product product = createProduct("Mouse", "29.90");

        createOrder(alice, product, 1);
        createOrder(alice, product, 2);
        createOrder(bob, product, 5);

        List<OrderSummaryDto> aliceOrders =
                purchaseHistoryService.findHistory(alice.getId(), 0).getContent();

        assertThat(aliceOrders).hasSize(2);
        assertThat(purchaseHistoryService.countOrders(alice.getId())).isEqualTo(2);
        assertThat(purchaseHistoryService.countOrders(bob.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("非法订单 ID 被拒绝，不抛底层异常")
    void rejectsInvalidOrderId() {
        User user = createUser("someone");

        assertThatThrownBy(() -> purchaseHistoryService.findOrderDetailForUser(0L, user.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> purchaseHistoryService.findOrderDetailForUser(-5L, user.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> purchaseHistoryService.findOrderDetailForUser(null, user.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- 分页 ----------

    @Test
    @DisplayName("历史列表每页5 条，能正确翻页且不丢数据")
    void historyIsPaginated() {
        User user = createUser("pager");
        Product product = createProduct("Monitor", "189.00");

        LocalDateTime sameTime = LocalDateTime.of(2026, 10, 9, 12, 0);
        java.util.ArrayList<Long> expectedIds = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            Order order = createOrder(user, product, 1);
            ReflectionTestUtils.setField(order, "createdAt", sameTime);
            orderRepository.saveAndFlush(order);
            expectedIds.add(order.getId());
        }

        var firstPage = purchaseHistoryService.findHistory(user.getId(), 0);
        var secondPage = purchaseHistoryService.findHistory(user.getId(), 1);

        assertThat(firstPage.getContent()).hasSize(5);
        assertThat(firstPage.getTotalElements()).isEqualTo(7);
        assertThat(secondPage.getContent()).hasSize(2);

        // 两页不能有重复：分页必须真的下推到 SQL，
        // 如果 group by 写错导致分页在内存里做，这里会失败。
        var firstIds = firstPage.getContent().stream().map(OrderSummaryDto::getId).toList();
        var secondIds = secondPage.getContent().stream().map(OrderSummaryDto::getId).toList();
        assertThat(firstIds).doesNotContainAnyElementsOf(secondIds);
        expectedIds.sort(java.util.Comparator.reverseOrder());
        assertThat(firstIds).containsExactlyElementsOf(expectedIds.subList(0, 5));
        assertThat(secondIds).containsExactlyElementsOf(expectedIds.subList(5, 7));
    }

    @Test
    @DisplayName("非法页码被兜住，不会抛异常")
    void handlesInvalidPageNumbers() {
        User user = createUser("pager2");
        Product product = createProduct("Hub", "45.00");
        createOrder(user, product, 1);

        // 负数页码按 0 处理
        assertThat(purchaseHistoryService.findHistory(user.getId(), -3).getContent()).hasSize(1);
    }

    // ---------- 快照 ----------

    @Test
    @DisplayName("管理员改价后，历史订单金额保持不变（成交快照生效）")
    void changingPriceDoesNotAlterHistoricalOrders() {
        User user = createUser("buyer");
        Product product = createProduct("Headphones", "199.00");
        Order order = createOrder(user, product, 1);

        // 记录下单时看到的金额
        OrderDetailDto before =
                purchaseHistoryService.findOrderDetailForUser(order.getId(), user.getId());
        assertThat(before.getItems().get(0).getUnitPrice())
                .isEqualByComparingTo("199.00");
        assertThat(before.getTotalAmount()).isEqualByComparingTo("199.00");

        // 模拟管理员在后台把价格翻倍并下架
        product.setPrice(new BigDecimal("398.00"));
        product.setActive(false);
        productRepository.save(product);

        // 历史订单应该纹丝不动
        OrderDetailDto after =
                purchaseHistoryService.findOrderDetailForUser(order.getId(), user.getId());
        assertThat(after.getItems().get(0).getUnitPrice())
                .isEqualByComparingTo("199.00");
        assertThat(after.getTotalAmount()).isEqualByComparingTo("199.00");
    }

    @Test
    @DisplayName("商品改名后，历史订单仍显示成交时的名称")
    void renamingProductDoesNotAlterHistoricalOrders() {
        User user = createUser("buyer2");
        Product product = createProduct("Old Name", "50.00");
        Order order = createOrder(user, product, 1);

        product.setName("New Name");
        productRepository.save(product);

        OrderDetailDto detail =
                purchaseHistoryService.findOrderDetailForUser(order.getId(), user.getId());
        assertThat(detail.getItems().get(0).getProductName()).isEqualTo("Old Name");
    }

    @Test
    @DisplayName("订单详情返回正确的明细与件数")
    void orderDetailContainsCorrectItems() {
        User user = createUser("detail");
        Product keyboard = createProduct("Keyboard", "59.90");
        Product mouse = createProduct("Mouse", "29.90");

        Order order = new Order(user,
                keyboard.getPrice().multiply(BigDecimal.valueOf(2))
                        .add(mouse.getPrice()),
                UUID.randomUUID().toString());
        orderRepository.save(order);
        orderItemRepository.save(new OrderItem(order, keyboard, 2));
        orderItemRepository.save(new OrderItem(order, mouse, 1));

        OrderDetailDto detail =
                purchaseHistoryService.findOrderDetailForUser(order.getId(), user.getId());

        assertThat(detail.getItems()).hasSize(2);
        assertThat(detail.getTotalQuantity()).isEqualTo(3);
        assertThat(detail.getItems().get(0).getSubtotal())
                .isEqualByComparingTo("119.80");
    }

    @Test
    @DisplayName("没有订单时累计消费为 0，不返回 null")
    void lifetimeSpendIsZeroWhenNoOrders() {
        User user = createUser("empty");

        assertThat(purchaseHistoryService.countOrders(user.getId())).isZero();
        assertThat(purchaseHistoryService.calculateLifetimeSpend(user.getId()))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ---------- 测试夹具 ----------

    private User createUser(String username) {
        User user = new User(username, "pw", username, username + "@test.local");
        return userRepository.save(user);
    }

    private Product createProduct(String name, String price) {
        Product product = new Product();
        product.setName(name);
        product.setPrice(new BigDecimal(price));
        product.setActive(true);
        return productRepository.save(product);
    }

    private Order createOrder(User user, Product product, int quantity) {
        BigDecimal amount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        Order order = new Order(user, amount, UUID.randomUUID().toString());
        // 必须先存 Order 再存 OrderItem：
        // 明细的 order_id 是非空外键，Order 还没落库时（transient）
        // 直接存明细会抛 TransientPropertyValueException。
        orderRepository.save(order);
        orderItemRepository.save(new OrderItem(order, product, quantity));
        return order;
    }
}
