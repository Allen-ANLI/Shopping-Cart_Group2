package sg.edu.nus.iss.shoppingcart.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.service.ProductService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
import sg.edu.nus.iss.shoppingcart.dto.ProductPageResponse;

import java.util.List;

/**
 * 接收商品列表、分页和详情请求，校验参数并返回商品查询结果。
 * @author 王重一
 */
@RestController
@RequestMapping("/api/products")
public class ProductRestController {

    private final ProductService productService;

    public ProductRestController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping(params = "!page")
    public List<Product> findAllProducts() {
        return productService.findAllProducts();
    }

    @GetMapping(params = "page")
    public ProductPageResponse findProductPage(
            @RequestParam(name = "page")
            @Min(0) int page,

            @RequestParam(name = "size", defaultValue = "6")
            @Min(1) @Max(100) int size,
            @RequestParam(name = "q", defaultValue = "") @Size(max = 100) String query,
            @RequestParam(name = "sort", defaultValue = "featured")
            @Pattern(regexp = "featured|price-asc|price-desc") String sort) {

        Page<Product> result =
                query.isBlank() && sort.equals("featured")
                        ? productService.findProductPage(page, size)
                        : productService.findProductPage(page, size, query, sort);

        return new ProductPageResponse(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> findProductById(
            @PathVariable("id") Long id) {

        return ResponseEntity.of(
                productService.findProductById(id));
    }
}