package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import java.util.Map;

/** Localizes the original modules' shared status messages without translating user content. */
@Component("uiText")
public class UiText {
    private static final Map<String,String> CHINESE = Map.ofEntries(
        Map.entry("Product added to your cart", "商品已加入购物车"),
        Map.entry("Cart updated", "购物车已更新"), Map.entry("Product removed", "商品已移除"),
        Map.entry("Cart cleared", "购物车已清空"), Map.entry("Order not found", "找不到此订单"),
        Map.entry("Product not found", "找不到此商品"), Map.entry("Not found", "未找到"),
        Map.entry("Bad request", "请求无效"), Map.entry("Server error", "服务暂不可用"),
        Map.entry("Action not allowed", "无法完成此操作"),
        Map.entry("The requested resource was not found.", "找不到请求的页面或资源。"),
        Map.entry("The request was not valid.", "请求信息无效，请检查后重试。"),
        Map.entry("Something went wrong on our side. Please try again.", "服务暂时出现问题，请稍后重试。"),
        Map.entry("This action must be submitted using its form.", "请使用页面中的表单完成此操作。"),
        Map.entry("Please log in to continue", "请先登录再继续"),
        Map.entry("Please check the information you entered", "请检查你填写的信息"),
        Map.entry("This product is not in your cart", "购物车中没有此商品"),
        Map.entry("Your shopping cart is empty", "购物车为空，请先添加商品"),
        Map.entry("Please select a valid product", "请选择有效商品"),
        Map.entry("This product is no longer available", "此商品已下架或不可购买"),
        Map.entry("Quantity must be between 0 and 99; use 0 to remove an item", "数量须为 0 至 99，设为 0 可移除商品"),
        Map.entry("Quantity must be between 1 and 99", "数量须为 1 至 99"),
        Map.entry("This form has expired. Refresh the page and try again.", "表单已过期，请刷新页面后重试。"),
        Map.entry("Enter a valid product ID and a whole-number quantity from 0 to 99", "请输入有效商品及 0 至 99 的整数数量"),
        Map.entry("Your cart changed or this checkout form expired. Review it and try again.", "购物车已变化或结账表单已过期，请重新确认后提交。"),
        Map.entry("This checkout receipt has expired", "此结账回执已过期，请到购买历史查看订单"),
        Map.entry("This product has been removed. Please remove it from your cart.", "商品已被删除，请将它从购物车中移除。"),
        Map.entry("This product is no longer for sale. Please remove it.", "此商品已下架，请将它从购物车中移除。")
    );
    public String translate(String text) { return localize(text); }
    public static String localize(String text) {
        if (text == null) return "";
        return LocaleContextHolder.getLocale().getLanguage().equals("zh") ? CHINESE.getOrDefault(text, text) : text;
    }
}
