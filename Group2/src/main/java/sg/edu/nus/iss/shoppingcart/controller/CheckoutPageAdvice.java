package sg.edu.nus.iss.shoppingcart.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.exception.NotAuthenticatedException;

/**
 * D 的结账页面身份与业务错误处理。
 * @author 邱弈杰
 */
@ControllerAdvice(assignableTypes = CheckoutController.class)
public class CheckoutPageAdvice {
    @ExceptionHandler(NotAuthenticatedException.class)
    public String loginRequired() { return "redirect:/login?required"; }

    @ExceptionHandler(BusinessException.class)
    public String businessFailure(BusinessException ex, RedirectAttributes flash) {
        flash.addFlashAttribute("errorMessage", ex.getUserMessage());
        return "redirect:/cart";
    }
}
