package sg.edu.nus.iss.shoppingcart.interceptor;

import jakarta.servlet.http.HttpSession;

/**
 * 当前登录用户上下文 —— E 读取身份的<b>唯一</b>入口。
 *
 * <p><b>为什么强调唯一：</b>分工文档要求"当前用户身份必须从服务器会话中取得"。
 * 如果 E 在 Controller 里接受一个 {@code userId} 请求参数，
 * 那就是致命的越权漏洞——攻击者只要改成别人的 ID 就能看别人的订单。</p>
 *
 * <p>所以这里只暴露"从 Session 取 ID"的方法，没有"设置"或"覆盖"的方法。
 * Session 属性名 {@code loginUserId} 与 B 的约定一致，合并时不会冲突。</p>
 *
 * @author 蔡千一（Module E）
 */
public final class CurrentUser {

    /** Session 中保存当前用户 ID 的属性名，由 B 的登录流程写入。 */
    public static final String LOGIN_USER_ID = "loginUserId";

    /** 禁止实例化。 */
    private CurrentUser() {
    }

    /**
     * 从 Session 取当前登录用户 ID。
     *
     * <p>不查数据库、不加载完整User 对象：E 的归属校验只需要 ID，
     * 每次请求为了显示一个问候语多打一次数据库并不划算。
     * 需要用户名或角色时，由拦截器按ID 单独重读。</p>
     *
     * @param session HTTP 会话
     * @return 用户 ID；未登录、会话已失效或属性类型不符时返回 null
     */
    public static Long getId(HttpSession session) {
        if (session == null) {
            return null;
        }
        try {
            Object value = session.getAttribute(LOGIN_USER_ID);
            // 类型严格校验：只认 Long。
            // 误存 String 或 Integer 时返回 null 走未登录分支，
            // 而不是抛 ClassCastException 让用户看到 500。
            return (value instanceof Long userId && userId > 0) ? userId : null;
        } catch (IllegalStateException ex) {
            // 会话已被 invalidate：旧请求继续执行就会命中这里，不能让它变成 500。
            return null;
        }
    }
}