package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import sg.edu.nus.iss.shoppingcart.service.*;
import sg.edu.nus.iss.shoppingcart.interceptor.CurrentUser;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;

@Controller
public class OrderPaymentController {
    private final PurchaseHistoryService history;
    private final OrderPaymentService payments;
    private final CartService cart;
    private final PaymentReceiptPdfService receipts;
    public OrderPaymentController(PurchaseHistoryService history,OrderPaymentService payments,CartService cart,PaymentReceiptPdfService receipts) {
        this.history=history;this.payments=payments;this.cart=cart;this.receipts=receipts;
    }
    @GetMapping("/orders/{id}/payment")
    public String page(@PathVariable Long id,HttpSession session,Model model) {
        var order=history.findOrderDetailForUser(id,CurrentUser.getId(session));
        if(!order.isPendingPayment()) return "redirect:/orders/"+id;
        model.addAttribute("order",order);model.addAttribute("cartFormToken",cart.formToken(session));
        return "orders/payment";
    }
    @PostMapping("/orders/{id}/payment")
    public String pay(@PathVariable Long id,@RequestParam(required=false) String cartFormToken,
                      @RequestParam(defaultValue="VISA") String paymentMethod,
                      @RequestParam(defaultValue="") String cardholderName,@RequestParam(defaultValue="") String cardNumber,
                      @RequestParam(defaultValue="") String cardExpiry,@RequestParam(defaultValue="") String cardSecurityCode,
                      HttpSession session,Model model) {
        synchronized(session) {
            try { cart.validateFormToken(session,cartFormToken); }
            catch(BusinessException ex) { throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN,UiText.localize(ex.getMessage())); }
            try {
                payments.pay(id,CurrentUser.getId(session),new PaymentService.Request(paymentMethod,"APPROVED",cardholderName,cardNumber,cardExpiry,cardSecurityCode,null,null,null,null));
                return "redirect:/orders/"+id+"?paid";
            } catch(BusinessException | IllegalArgumentException ex) { model.addAttribute("errorMessage",ex.getMessage()); }
        }
        model.addAttribute("selectedPaymentMethod",paymentMethod);
        return page(id,session,model);
    }
    @GetMapping("/orders/{id}/receipt.pdf")
    public ResponseEntity<byte[]> download(@PathVariable Long id,HttpSession session) {
        var order=history.findOrderDetailForUser(id,CurrentUser.getId(session));
        if(order.isPendingPayment()) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT,"Payment is required before downloading a receipt");
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"NEXUS-Receipt-"+id+".pdf\"")
                .cacheControl(CacheControl.noStore()).body(receipts.create(order));
    }
}
