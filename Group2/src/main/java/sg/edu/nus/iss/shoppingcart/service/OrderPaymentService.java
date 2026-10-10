package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sg.edu.nus.iss.shoppingcart.entity.*;
import sg.edu.nus.iss.shoppingcart.repository.*;
import sg.edu.nus.iss.shoppingcart.exception.*;
import java.util.*;

/** Pays an existing order once, atomically checking and decrementing current inventory. */
@Service
public class OrderPaymentService {
    private final OrderRepository orders;
    private final OrderItemRepository items;
    private final ProductService products;
    private final PaymentService gateway;
    public OrderPaymentService(OrderRepository orders, OrderItemRepository items, ProductService products, PaymentService gateway) {
        this.orders=orders; this.items=items; this.products=products; this.gateway=gateway;
    }
    @Transactional
    public void pay(Long orderId, Long userId, PaymentService.Request request) {
        if (userId == null) throw new NotAuthenticatedException();
        var order=orders.findForUserUpdate(orderId,userId).orElseThrow(ResourceNotFoundException::orderNotAccessible);
        if (order.isPaid()) return; // Retries never charge or decrement inventory twice.
        if (!order.isPendingPayment()) throw new BusinessException(PaymentService.localized("This order cannot be paid.", "此订单无法支付。"));
        if (request == null || !("VISA".equals(request.method()) || "MASTERCARD".equals(request.method())))
            throw new BusinessException(PaymentService.localized("Please select Visa or Mastercard.", "请选择 Visa 或 Mastercard。"));
        var lines=items.findByOrder_IdOrderByIdAsc(orderId).stream().sorted(Comparator.comparing(i->i.getProduct().getId())).toList();
        var inventory=new LinkedHashMap<Product,Integer>();
        for(var line:lines) {
            var product=products.findForCheckout(line.getProduct().getId()).orElseThrow(()->new BusinessException(PaymentService.localized("An item is no longer available.","部分商品已下架。")));
            int quantity=inventory.getOrDefault(product,0)+line.getQuantity();
            if(quantity>product.getStockQuantity()) throw new BusinessException(PaymentService.localized("Not enough stock. Please try again after restocking.","商品库存不足，请补货后重试。"));
            inventory.put(product,quantity);
        }
        var approved=new PaymentService.Request(request.method(),"APPROVED",request.cardholderName(),request.cardNumber(),request.cardExpiry(),request.cardSecurityCode(),null,null,null,null);
        var receipt=gateway.pay(order.getTotalAmount(),order.getCheckoutToken(),approved);
        order.recordPayment(receipt.method(),receipt.reference());
        order.recordPaymentInstrument(receipt.instrument(),receipt.lastDigits(),null,null,null);
        inventory.forEach((product,quantity)->product.setStockQuantity(product.getStockQuantity()-quantity));
    }
}
