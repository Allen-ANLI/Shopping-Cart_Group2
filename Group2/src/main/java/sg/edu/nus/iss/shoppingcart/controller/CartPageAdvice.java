package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.exception.NotAuthenticatedException;
import sg.edu.nus.iss.shoppingcart.service.CartService;

/**
 * 购物车页面的令牌和错误提示；结账请求由 D 的独立处理器负责。
 * @author Letian Xie
 */
@ControllerAdvice(assignableTypes = {CartController.class,
        CartProductPageController.class})
public class CartPageAdvice {
    private final CartService cart;
    public CartPageAdvice(CartService cart) { this.cart = cart; }

    @ModelAttribute
    public void formToken(HttpSession session, Model model) {
        model.addAttribute("cartFormToken", cart.formToken(session));
    }

    @ExceptionHandler(NotAuthenticatedException.class)
    public String loginRequired() { return "redirect:/login?required"; }

    @ExceptionHandler(BusinessException.class)
    public String businessFailure(BusinessException ex, RedirectAttributes flash) {
        flash.addFlashAttribute("errorMessage", ex.getUserMessage());
        return "redirect:/cart";
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public String invalidProduct(RedirectAttributes flash) {
        flash.addFlashAttribute("errorMessage", "Please select a valid product");
        return "redirect:/cart";
    }
}
