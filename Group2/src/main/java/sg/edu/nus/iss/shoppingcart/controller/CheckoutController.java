package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponentsBuilder;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.exception.NotAuthenticatedException;
import sg.edu.nus.iss.shoppingcart.service.CartService;
import sg.edu.nus.iss.shoppingcart.service.CheckoutCoordinator;
import sg.edu.nus.iss.shoppingcart.service.CheckoutReceiptService;

/**
 * 接收结账请求并组织确认页面与订单回执。
 * @author 邱弈杰
 */
@Controller
public class CheckoutController {
    private static final Logger log = LoggerFactory.getLogger(CheckoutController.class);
    private final CartService cartService;
    private final CheckoutCoordinator coordinator;
    private final CheckoutReceiptService receipts;
    private final sg.edu.nus.iss.shoppingcart.service.ShippingAddressService addresses;

    public CheckoutController(CartService cartService, CheckoutCoordinator coordinator,
                              CheckoutReceiptService receipts,
                              sg.edu.nus.iss.shoppingcart.service.ShippingAddressService addresses) {
        this.cartService = cartService;
        this.coordinator = coordinator;
        this.receipts = receipts;
        this.addresses = addresses;
    }

    @GetMapping("/checkout")
    public String show(HttpSession session, Model model) {
        populate(session, model);
        return "orders/checkout";
    }

    @PostMapping("/checkout")
    public String submit(@RequestParam(required = false) String checkoutToken,
                         @RequestParam(required = false) Long addressId,
                         HttpSession session, Model model) {
        try {
            if (addressId == null) {
                throw new BusinessException(org.springframework.context.i18n.LocaleContextHolder.getLocale()
                        .getLanguage().equals("zh") ? "请选择收货地址" : "Please select a delivery address");
            }
            coordinator.submit(session, checkoutToken, addressId);
            return "redirect:" + UriComponentsBuilder.fromPath("/checkout/success")
                    .queryParam("key", checkoutToken).build().encode().toUriString();
        } catch (NotAuthenticatedException ex) {
            throw ex;
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            model.addAttribute("errorMessage", ex.getReason());
        } catch (BusinessException | IllegalArgumentException ex) {
            model.addAttribute("errorMessage", ex.getMessage() + (org.springframework.context.i18n.LocaleContextHolder.getLocale()
                    .getLanguage().equals("zh") ? "。购物车已保留。" : ". Your cart has been kept."));
        } catch (RuntimeException ex) {
            log.error("Checkout failed; keeping the session cart", ex);
            model.addAttribute("errorMessage", org.springframework.context.i18n.LocaleContextHolder.getLocale()
                    .getLanguage().equals("zh") ? "下单失败，购物车已保留，请重试。" : "We could not place your order. Your cart has been kept. Please try again.");
        }
        populate(session, model);
        model.addAttribute("selectedAddressId", addressId);
        return "orders/checkout";
    }

    @GetMapping("/checkout/success")
    public String success(@RequestParam String key, HttpSession session, Model model) {
        synchronized (session) {
            Long userId = cartService.requireUserId(session);
            coordinator.completedOrder(session, key);
            model.addAttribute("receipt", receipts.forUser(key, userId));
        }
        return "orders/checkout-success";
    }

    private void populate(HttpSession session, Model model) {
        synchronized (session) {
            var items = cartService.getCartItems(session);
            model.addAttribute("cartItems", items);
            model.addAttribute("cartTotal", cartService.calculateTotal(items));
            model.addAttribute("totalQuantity", cartService.countTotalQuantity(items));
            model.addAttribute("canCheckout", cartService.canCheckout(items));
            model.addAttribute("checkoutToken", coordinator.prepare(session));
            var shippingAddresses = addresses.listForUser(cartService.requireUserId(session));
            model.addAttribute("addresses", shippingAddresses);
            model.addAttribute("selectedAddressId", shippingAddresses.stream()
                    .filter(sg.edu.nus.iss.shoppingcart.entity.ShippingAddress::isDefaultAddress)
                    .findFirst().or(() -> shippingAddresses.stream().findFirst())
                    .map(sg.edu.nus.iss.shoppingcart.entity.ShippingAddress::getId).orElse(null));
        }
    }
}
