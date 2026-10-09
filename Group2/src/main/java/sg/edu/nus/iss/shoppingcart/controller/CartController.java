package sg.edu.nus.iss.shoppingcart.controller;

import sg.edu.nus.iss.shoppingcart.dto.CartLine;
import sg.edu.nus.iss.shoppingcart.dto.CartQuantityForm;
import sg.edu.nus.iss.shoppingcart.dto.CartProductForm;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.service.CartService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * Session 购物车页面与写操作。
 * @author Letian Xie
 */
@Controller
public class CartController {
    private final CartService cartService;

    public CartController(CartService cartService) { this.cartService = cartService; }

    @GetMapping("/cart")
    public String viewCart(HttpSession session, Model model) {
        List<CartLine> items = cartService.getCartItems(session);
        model.addAttribute("cartItems", items);
        model.addAttribute("cartTotal", cartService.calculateTotal(items));
        model.addAttribute("totalQuantity", cartService.countTotalQuantity(items));
        model.addAttribute("canCheckout", cartService.canCheckout(items));
        return "cart/view";
    }

    @PostMapping("/cart/add")
    public String addToCart(@Valid @ModelAttribute CartQuantityForm form, BindingResult result,
                           @RequestParam(required = false) String cartFormToken,
                           HttpSession session, RedirectAttributes flash) {
        return change(session, cartFormToken, result, flash,
                () -> cartService.addItem(session, form.getProductId(), form.getQuantity()),
                "Product added to your cart");
    }

    @PostMapping("/cart/update")
    public String updateQuantity(@Valid @ModelAttribute CartQuantityForm form, BindingResult result,
                                 @RequestParam(required = false) String cartFormToken,
                                 HttpSession session, RedirectAttributes flash) {
        return change(session, cartFormToken, result, flash,
                () -> cartService.updateQuantity(session, form.getProductId(), form.getQuantity()),
                "Cart updated");
    }

    @PostMapping("/cart/remove")
    public String removeItem(@Valid @ModelAttribute CartProductForm form, BindingResult result,
                             @RequestParam(required = false) String cartFormToken,
                             HttpSession session, RedirectAttributes flash) {
        return change(session, cartFormToken, result, flash,
                () -> cartService.removeItem(session, form.getProductId()), "Product removed");
    }

    @PostMapping("/cart/clear")
    public String clearCart(@RequestParam(required = false) String cartFormToken,
                            HttpSession session, RedirectAttributes flash) {
        return change(session, cartFormToken, null, flash,
                () -> cartService.clearCart(session), "Cart cleared");
    }

    private String change(HttpSession session, String token, BindingResult result,
                          RedirectAttributes flash, Runnable action, String success) {
        synchronized (session) {
            try {
                cartService.validateFormToken(session, token);
                if (result != null && result.hasErrors()) {
                    throw new BusinessException("Enter a valid product ID and a whole-number quantity from 0 to 99");
                }
                action.run();
                flash.addFlashAttribute("successMessage", success);
            } catch (BusinessException ex) {
                flash.addFlashAttribute("errorMessage", ex.getUserMessage());
            }
            return "redirect:/cart";
        }
    }
}
