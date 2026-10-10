package sg.edu.nus.iss.shoppingcart.dto;

import sg.edu.nus.iss.shoppingcart.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Safe receipt projection: never contains full card numbers, PINs or security codes. */
public record PaymentDetails(String status, String method, String reference, LocalDateTime paidAt,
                             String instrument, String lastDigits, String cryptoAsset,
                             String cryptoNetwork, BigDecimal cryptoAmount) {
    public static PaymentDetails from(Order order) {
        return new PaymentDetails(order.getPaymentStatus(), order.getPaymentMethod(), order.getPaymentReference(), order.getPaidAt(),
                order.getPaymentInstrument(), order.getPaymentLastDigits(), order.getCryptoAsset(), order.getCryptoNetwork(), order.getCryptoAmount());
    }
}
