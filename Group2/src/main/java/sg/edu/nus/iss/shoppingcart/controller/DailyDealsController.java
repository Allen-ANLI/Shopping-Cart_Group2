package sg.edu.nus.iss.shoppingcart.controller;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import sg.edu.nus.iss.shoppingcart.entity.Product;
import sg.edu.nus.iss.shoppingcart.service.ProductService;

@RestController
public class DailyDealsController {
    private final ProductService products;
    public DailyDealsController(ProductService products) { this.products = products; }
    @GetMapping("/api/deals")
    public ResponseEntity<DailyDeals> read() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new DailyDeals(
                LocalDate.now(ZoneId.of("Asia/Singapore")), products.dailyDeals()));
    }
    public record DailyDeals(LocalDate date, List<Product> products) {}
}
