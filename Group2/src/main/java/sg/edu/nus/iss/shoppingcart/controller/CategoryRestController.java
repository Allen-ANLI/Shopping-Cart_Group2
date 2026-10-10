package sg.edu.nus.iss.shoppingcart.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sg.edu.nus.iss.shoppingcart.service.ProductService;
import java.util.List;

@RestController
public class CategoryRestController {
    private final ProductService products;
    public CategoryRestController(ProductService products) { this.products = products; }
    @GetMapping("/api/categories")
    public List<ProductService.CategorySummary> categories() { return products.categories(); }
}
