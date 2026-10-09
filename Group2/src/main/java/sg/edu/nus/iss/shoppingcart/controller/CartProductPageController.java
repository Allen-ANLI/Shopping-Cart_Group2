package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.service.CartService;
import sg.edu.nus.iss.shoppingcart.service.ProductService;

import java.util.List;

/**
 * Thymeleaf 加购入口。Angular 匿名用户登录后可回到指定商品的数量表单。
 * @author Letian Xie
 */
@Controller
public class CartProductPageController {
    private final CartService cart;
    private final ProductService products;
    public CartProductPageController(CartService cart, ProductService products) {
        this.cart = cart;
        this.products = products;
    }
    @GetMapping("/cart/products")
    public String show(@RequestParam(required = false) Long productId, HttpSession session, Model model) {
        cart.requireUserId(session);
        if (productId != null) {
            if (productId <= 0) { throw new BusinessException("Please select a valid product"); }
            var product = products.findProductById(productId)
                    .orElseThrow(() -> new BusinessException("This product is no longer available"));
            model.addAttribute("products", List.of(product));
        } else {
            model.addAttribute("products", products.findAllProducts());
        }
        model.addAttribute("selectedProduct", productId != null);
        return "cart/products";
    }
}
