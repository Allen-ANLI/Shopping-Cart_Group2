package sg.edu.nus.iss.shoppingcart.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 提供商品浏览页面入口，将请求转发到 Angular 构建后的静态页面。
 * @author 王重一
 */
@Controller
public class ProductPageController {

    @GetMapping("/products")
    public String showProducts() {
        return "forward:/products/index.html";
    }
}