package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sg.edu.nus.iss.shoppingcart.entity.Order;
import sg.edu.nus.iss.shoppingcart.exception.ResourceNotFoundException;
import sg.edu.nus.iss.shoppingcart.repository.OrderItemRepository;
import sg.edu.nus.iss.shoppingcart.repository.OrderRepository;

/** Owns the delivered -> customer-confirmed -> review flow. */
@Service
public class OrderCompletionService {
    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final ProductReviewService reviews;
    public OrderCompletionService(OrderRepository orders, OrderItemRepository items, ProductReviewService reviews) {
        this.orders = orders; this.items = items; this.reviews = reviews;
    }
    @Transactional
    public void confirm(Long orderId, Long userId) {
        Order order=owned(orderId, userId);
        if (!order.isPaid()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, PaymentService.localized("Pay for your order before confirming receipt.", "请先完成支付，再确认收货。"));
        order.confirmReceipt();
    }
    @Transactional
    public void review(Long orderId, Long productId, Long userId, int rating, String comment) {
        Order order = owned(orderId, userId);
        if (!order.isReceiptConfirmed()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                PaymentService.localized("Confirm receipt before writing a review.", "请先确认收货，再进行评价。"));
        if (items.findByOrder_IdOrderByIdAsc(orderId).stream().noneMatch(item -> item.getProduct().getId().equals(productId)))
            throw ResourceNotFoundException.of("Product", productId);
        reviews.save(productId, userId, rating, comment);
    }
    private Order owned(Long id, Long userId) {
        if (userId == null) throw new sg.edu.nus.iss.shoppingcart.exception.NotAuthenticatedException();
        return orders.findForUserUpdate(id, userId).orElseThrow(ResourceNotFoundException::orderNotAccessible);
    }
}
