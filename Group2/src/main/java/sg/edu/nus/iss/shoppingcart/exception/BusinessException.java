package sg.edu.nus.iss.shoppingcart.exception;

/**
 * 业务异常基类。
 *
 * <p>用于表达"可预期的用户操作错误"，例如商品不存在、库存不足。
 * 由 {@link GlobalExceptionHandler} 统一捕获并转成友好页面，
 * 而不是把堆栈直接抛给用户。</p>
 *
 * @author CA Project Team
 * @since 1.0
 */
public class BusinessException extends RuntimeException {

    /** 错误提示信息，展示给用户。 */
    private final String userMessage;

    /**
     * 构造业务异常。
     *
     * @param userMessage 展示给用户的友好提示
     */
    public BusinessException(String userMessage) {
        super(userMessage);
        this.userMessage = userMessage;
    }

    /**
     * 构造带内部原因的异常。内部原因仅写入日志，不展示给用户。
     *
     * @param userMessage  展示给用户的提示
     * @param internalCause 内部原因
     */
    public BusinessException(String userMessage, Throwable internalCause) {
        super(userMessage, internalCause);
        this.userMessage = userMessage;
    }

    /**
     * 获取展示给用户的提示。
     *
     * @return 友好提示
     */
    public String getUserMessage() {
        return userMessage;
    }
}
