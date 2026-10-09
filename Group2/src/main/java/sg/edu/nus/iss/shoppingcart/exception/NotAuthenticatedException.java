package sg.edu.nus.iss.shoppingcart.exception;

/**
 * 未登录异常。
 *
 * <p>登录拦截器检测到未登录用户访问受保护页面时抛出。</p>
 *
 * @author CA Project Team
 * @since 1.0
 */
public class NotAuthenticatedException extends BusinessException {

    /** 无参构造，使用固定提示。 */
    public NotAuthenticatedException() {
        super("Please log in to continue");
    }
}
