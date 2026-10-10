package sg.edu.nus.iss.shoppingcart;
import org.junit.jupiter.api.Test;
import sg.edu.nus.iss.shoppingcart.dto.*;
import sg.edu.nus.iss.shoppingcart.service.PaymentReceiptPdfService;
import java.util.*;import java.math.BigDecimal;import java.time.LocalDateTime;
import static org.assertj.core.api.Assertions.*;
class PaymentReceiptPdfTest {
 @Test void longOrdersPaginateAndRetainUnicodeAndTotals()throws Exception {
  var lines=new ArrayList<OrderDetailDto.OrderItemLine>();for(int n=1;n<=70;n++)lines.add(new OrderDetailDto.OrderItemLine("Product "+n+" / 办公显示器 - long product name for wrapping and pagination",new BigDecimal("19.90"),2));
  var order=new OrderDetailDto(42L,LocalDateTime.now(),new BigDecimal("2786.00"),lines);
  byte[] bytes=new PaymentReceiptPdfService().create(order);
  try(var document=org.apache.pdfbox.Loader.loadPDF(bytes)){assertThat(document.getNumberOfPages()).isGreaterThan(2);var text=new org.apache.pdfbox.text.PDFTextStripper().getText(document);assertThat(text).contains("Product 70","2786.00","显示器","TOTAL PAID");}
 }
}
