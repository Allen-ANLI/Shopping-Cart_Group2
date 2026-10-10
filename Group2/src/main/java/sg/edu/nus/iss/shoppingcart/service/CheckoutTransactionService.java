package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.entity.Order;
import sg.edu.nus.iss.shoppingcart.entity.OrderItem;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.repository.OrderItemRepository;
import sg.edu.nus.iss.shoppingcart.repository.OrderRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 在同一个事务中核价并保存订单和明细。
 * @author 邱弈杰
 */
@Service
public class CheckoutTransactionService {

    private final ProductService productService;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShippingAddressService addresses;

    public CheckoutTransactionService(
            ProductService productService,
            UserRepository userRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository, ShippingAddressService addresses) {
        this.productService = productService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.addresses = addresses;
    }

    @Transactional
    public Order createOrder(Long userId, Map<Long, Integer> cart,
                             String checkoutToken) {
        return createOrder(userId, cart, checkoutToken, null);
    }

    @Transactional
    public Order createOrder(Long userId, Map<Long, Integer> cart,
                             String checkoutToken, Long addressId) {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("购物车不能为空");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        BigDecimal total = BigDecimal.ZERO;
        Map<Product, Integer> checkedProducts = new LinkedHashMap<>();

        for (Map.Entry<Long, Integer> entry : cart.entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            if (productId == null || productId <= 0
                    || quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("商品 ID 和购买数量必须为正数");
            }

            Product product = productService.findProductById(productId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "商品不存在或已下架，商品 ID：" + productId));

            BigDecimal price = product.getPrice();
            if (price == null || price.signum() < 0) {
                throw new IllegalArgumentException("商品价格不合法");
            }

            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));
            total = total.add(subtotal);
            checkedProducts.put(product, quantity);
        }

        // 对应 Order.totalAmount 的 DECIMAL(14, 2) 范围。
        if (total.compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new IllegalArgumentException("订单总金额超出允许范围");
        }

        var shipping = addressId == null ? null : new sg.edu.nus.iss.shoppingcart.entity.ShippingSnapshot(
                addresses.requireForUser(addressId, userId));
        Order order = new Order(user, total, checkoutToken, shipping);
        order = orderRepository.saveAndFlush(order);

        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<Product, Integer> entry : checkedProducts.entrySet()) {
            items.add(new OrderItem(order, entry.getKey(), entry.getValue()));
        }
        orderItemRepository.saveAllAndFlush(items);

        return order;
    }
}
