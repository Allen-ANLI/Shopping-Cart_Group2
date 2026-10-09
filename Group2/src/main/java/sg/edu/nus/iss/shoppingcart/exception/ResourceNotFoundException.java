package sg.edu.nus.iss.shoppingcart.exception;

/**
 * 资源不存在异常。
 *
 * <p>E 只在一种情况下用：用户访问不属于自己（或根本不存在）的订单时。
 * 关键点在于<b>越权访问和订单不存在必须返回同一个提示</b>——
 * 如果一个用户改URL 就能发现"订单 5存在但不属于我"，
 * 就等于泄露了其他人的订单是否存在。所以这里不区分两种情况，
 * 统一按"找不到该订单"处理。</p>
 *
 * @author 蔡千一（Module E）
 */
public class ResourceNotFoundException extends RuntimeException {

    /** 可以直接展示给用户的提示。 */
    private final String userMessage;

    private ResourceNotFoundException(String userMessage) {
        super(userMessage);
        this.userMessage = userMessage;
    }

    /**
     * 按资源类型生成提示，例如 {@code of("Order", 5)} 得到"Order not found"。
     *
     * @param type 资源名称
     * @param id资源 ID
     * @return 异常实例
     */
    public static ResourceNotFoundException of(String type, Object id) {
        return new ResourceNotFoundException(type + " not found");
    }

    /**
     * 订单不存在或不属于当前用户的统一提示。
     *
     * @return 异常实例
     */
    public static ResourceNotFoundException orderNotAccessible() {
        return new ResourceNotFoundException("Order not found");
    }

    public String getUserMessage() {
        return userMessage;
    }
}