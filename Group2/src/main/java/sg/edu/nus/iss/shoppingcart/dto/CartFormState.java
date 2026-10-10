package sg.edu.nus.iss.shoppingcart.dto;

/**
 * Angular 加购表单需要的会话令牌和当前购物车计数，不包含价格或用户资料。
 * @author Letian Xie
 */
public record CartFormState(String cartFormToken, long itemCount, int totalQuantity, java.util.Map<Long, Integer> quantities) {}
