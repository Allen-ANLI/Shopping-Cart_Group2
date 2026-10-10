package sg.edu.nus.iss.shoppingcart.dto;
import java.math.BigDecimal;
import java.math.RoundingMode;
public record OrderPricing(BigDecimal originalSubtotal, BigDecimal discountAmount, BigDecimal subtotal, BigDecimal gstAmount, BigDecimal total) {
 public static OrderPricing calculate(BigDecimal original, BigDecimal discounted) {
  original = original.setScale(2, RoundingMode.HALF_UP); discounted = discounted.setScale(2, RoundingMode.HALF_UP);
  BigDecimal gst = discounted.multiply(new BigDecimal("0.09")).setScale(2, RoundingMode.HALF_UP);
  return new OrderPricing(original, original.subtract(discounted), discounted, gst, discounted.add(gst));
 }
}
