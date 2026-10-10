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
    private final PaymentService payments;

    public CheckoutTransactionService(
            ProductService productService,
            UserRepository userRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository, ShippingAddressService addresses, PaymentService payments) {
        this.productService = productService;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.addresses = addresses;
        this.payments = payments;
    }

    @Transactional
    public Order createOrder(Long userId, Map<Long, Integer> cart,
                             String checkoutToken) {
        return createOrder(userId, cart, checkoutToken, null);
    }

    @Transactional
    public Order createOrder(Long userId, Map<Long, Integer> cart,
                             String checkoutToken, Long addressId) {
        return createOrder(userId, cart, checkoutToken, addressId, PaymentService.Request.success());
    }

    @Transactional
    public Order createPendingOrder(Long userId, Map<Long, Integer> cart, String checkoutToken, Long addressId) {
        return createOrder(userId, cart, checkoutToken, addressId, null);
    }

    @Transactional
    public Order createOrder(Long userId, Map<Long, Integer> cart, String checkoutToken, Long addressId,
                             PaymentService.Request payment) {
        if (cart == null || cart.isEmpty()) {
            throw new IllegalArgumentException("购物车不能为空");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal original = BigDecimal.ZERO;
        Map<Product, Integer> checkedProducts = new LinkedHashMap<>();

        if (cart.keySet().stream().anyMatch(java.util.Objects::isNull)) throw new IllegalArgumentException("Invalid product ID");
        // A consistent lock order prevents overselling across sessions and reduces deadlock risk.
        for (Map.Entry<Long, Integer> entry : new java.util.TreeMap<>(cart).entrySet()) {
            Long productId = entry.getKey();
            Integer quantity = entry.getValue();

            if (productId == null || productId <= 0
                    || quantity == null || quantity <= 0 || quantity > CartService.MAX_QUANTITY) {
                throw new IllegalArgumentException("商品 ID 和购买数量必须为正数");
            }

            Product product = productService.findForCheckout(productId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "商品不存在或已下架，商品 ID：" + productId));
            if (quantity > product.getStockQuantity()) throw new sg.edu.nus.iss.shoppingcart.exception.BusinessException(
                    UiText.localize("Not enough stock. Please reduce the quantity."));

            BigDecimal price = product.getEffectivePrice();
            if (price == null || price.signum() < 0) {
                throw new IllegalArgumentException("商品价格不合法");
            }

            BigDecimal subtotal = price.multiply(BigDecimal.valueOf(quantity));
            total = total.add(subtotal);
            original = original.add(product.getPrice().multiply(BigDecimal.valueOf(quantity)));
            checkedProducts.put(product, quantity);
        }

        // 对应 Order.totalAmount 的 DECIMAL(14, 2) 范围。
        if (total.compareTo(new BigDecimal("999999999999.99")) > 0) {
            throw new IllegalArgumentException("订单总金额超出允许范围");
        }

        var shipping = addressId == null ? null : new sg.edu.nus.iss.shoppingcart.entity.ShippingSnapshot(
                addresses.requireForUser(addressId, userId));
        Order order = new Order(user, total, checkoutToken, shipping);
        if (payment == null) {
            order.awaitPayment(sg.edu.nus.iss.shoppingcart.dto.OrderPricing.calculate(original, total));
            if (order.getTotalAmount().compareTo(new BigDecimal("999999999999.99")) > 0) throw new IllegalArgumentException("Order total is too large");
        } else {
            PaymentService.Receipt paymentReceipt = payments.pay(total, checkoutToken, payment);
            order.recordPayment(paymentReceipt.method(), paymentReceipt.reference());
            order.recordPaymentInstrument(paymentReceipt.instrument(), paymentReceipt.lastDigits(), paymentReceipt.cryptoAsset(), paymentReceipt.cryptoNetwork(), paymentReceipt.cryptoAmount());
        }
        order = orderRepository.saveAndFlush(order);

        List<OrderItem> items = new ArrayList<>();
        for (Map.Entry<Product, Integer> entry : checkedProducts.entrySet()) {
            if (payment != null) entry.getKey().setStockQuantity(entry.getKey().getStockQuantity() - entry.getValue());
            items.add(new OrderItem(order, entry.getKey(), entry.getValue()));
        }
        orderItemRepository.saveAllAndFlush(items);

        return order;
    }
}
