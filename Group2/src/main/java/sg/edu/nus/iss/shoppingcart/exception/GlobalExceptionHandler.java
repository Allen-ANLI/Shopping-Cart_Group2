package sg.edu.nus.iss.shoppingcart.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 全局异常处理器 —— E 模块。
 *
 * <p>把三类异常翻译成用户能看懂的提示，而不是默认的 Whitelabel 错误页：</p>
 * <ul>
 *   <li>{@link ResourceNotFoundException} → 404 页。
 *       订单越权访问和订单不存在都走这里，<b>提示文案完全相同</b>，
 *       这样攻击者无法通过报错差异推断某个订单 ID 是否存在。</li>
 *   <li>{@link BusinessException} → 200 + 提示横幅。
 *       比如删除被引用的商品，属于操作被拒绝而不是资源不存在。</li>
 *   <li>{@link IllegalArgumentException} → 400 页，用于非法 URL 参数。</li>
 * </ul>
 *
 * <p>分工文档把"异常处理"列为硬性技术要求，这个类就是 E 的那一份。
 * 合并时各模块可以各自保留一份，或者由F 统一成一份全局处理器。</p>
 *
 * @author 蔡千一（Module E）
 * @author OpenAI Codex (HTTP error handling review)
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    /** 日志记录器。 */
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Preserve 404 for missing pages and assets instead of invoking the 500 fallback. */
    @ExceptionHandler(NoResourceFoundException.class)
    public Object handleMissingResource(NoResourceFoundException ex, HttpServletRequest request) {
        return requestError(request, HttpStatus.NOT_FOUND, "Not found", "The requested resource was not found.");
    }

    /** Spring MVC validates @Min/@Max before the controller method is invoked. */
    @ExceptionHandler({HandlerMethodValidationException.class, MissingServletRequestParameterException.class})
    public Object handleInvalidRequest(Exception ex, HttpServletRequest request) {
        HttpStatus status = ex instanceof HandlerMethodValidationException validation
                && validation.isForReturnValue() ? HttpStatus.INTERNAL_SERVER_ERROR : HttpStatus.BAD_REQUEST;
        return requestError(request, status, status == HttpStatus.BAD_REQUEST ? "Bad request" : "Server error",
                status == HttpStatus.BAD_REQUEST ? "The request was not valid." : "Something went wrong on our side.");
    }

    private Object requestError(HttpServletRequest request, HttpStatus status, String title, String message) {
        if (request.getRequestURI().substring(request.getContextPath().length()).startsWith("/api/")) {
            return ResponseEntity.status(status).body(Map.of("status", status.value(), "error", title, "message", message));
        }
        ModelAndView view = new ModelAndView("error/not-found");
        view.setStatus(status);
        view.addObject("errorTitle", title);
        view.addObject("errorMessage", message);
        return view;
    }

    /**
     * 资源不存在或不可访问。
     *
     * @param ex     异常
     * @param model  视图模型
     * @return 404 错误页
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(ResourceNotFoundException ex, Model model) {
        model.addAttribute("errorMessage", ex.getUserMessage());
        model.addAttribute("errorTitle", "Not found");
        return "error/not-found";
    }

    /**
     * 业务规则拒绝。
     *
     * <p>返回 200 而不是 4xx：这是用户点了按钮之后的正常反馈，
     * 浏览器刷新不会重复提交。真正的 HTTP 语义上它确实是错误，
     * 但对表单交互来说 200 + 提示更合适，也和项目里其他模块保持一致。</p>
     *
     * @param ex    异常
     * @param model 视图模型
     * @return 提示页
     */
    @ExceptionHandler(BusinessException.class)
    public String handleBusiness(BusinessException ex, Model model) {
        model.addAttribute("errorMessage", ex.getUserMessage());
        model.addAttribute("errorTitle", "Action not allowed");
        return "error/business";
    }

    /**
     * 非法请求参数。
     *
     * <p>两种情况分开处理，因为它们不是同一个异常类：</p>
     * <ul>
     *   <li>{@link IllegalArgumentException} —— 业务代码自己抛的，
     *       比如 Service 发现订单 ID 小于等于 0。</li>
     *   <li>{@link org.springframework.web.method.annotation.MethodArgumentTypeMismatchException}
     *       —— URL 里的值根本不是数字，比如 {@code /orders/abc}。
     *       Spring 把它包装成这个类型，<b>不是</b> IllegalArgumentException，
     *       所以必须单独声明处理器，否则会掉进兜底的 500 分支：
     *       用户敲错一个 URL 就看到"服务器错误"，是很糟的体验。</li>
     * </ul>
     *
     * @param ex    异常
     * @param model 视图模型
     * @return 400 错误页
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class
    })
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgument(Exception ex, Model model) {
        model.addAttribute("errorMessage", "The request was not valid.");
        model.addAttribute("errorTitle", "Bad request");
        return "error/not-found";
    }

    /**
     * 兜底处理器。
     *
     * <p>记录完整堆栈到日志，但只给用户一句笼统的话——
     * 异常细节可能包含 SQL 或表结构，不该出现在页面上。</p>
     *
     * @param ex    异常
     * @param model 视图模型
     * @return 500 错误页
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpected(Exception ex, Model model) {
        log.error("Unexpected error while handling request", ex);
        model.addAttribute("errorMessage",
                "Something went wrong on our side. Please try again.");
        model.addAttribute("errorTitle", "Server error");
        return "error/not-found";
    }
}
