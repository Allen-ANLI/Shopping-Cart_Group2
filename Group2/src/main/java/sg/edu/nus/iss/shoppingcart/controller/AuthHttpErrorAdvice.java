package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 保留 HTTP 405 语义，避免 GET /logout 被全局兜底误判为服务器错误。
 * @author luopeiwen
 */
@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthHttpErrorAdvice {
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    @ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
    public String methodNotAllowed(HttpRequestMethodNotSupportedException ex,
                                   HttpServletResponse response, Model model) {
        response.setHeader("Cache-Control", "no-store");
        if (ex.getSupportedMethods() != null) {
            response.setHeader("Allow", String.join(", ", ex.getSupportedMethods()));
        }
        model.addAttribute("errorMessage", "This action must be submitted using its form.");
        return "auth/request-error";
    }
}
