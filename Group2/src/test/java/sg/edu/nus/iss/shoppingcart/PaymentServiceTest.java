package sg.edu.nus.iss.shoppingcart;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;
import sg.edu.nus.iss.shoppingcart.service.PaymentService;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import java.math.BigDecimal;
import java.util.Locale;
import static org.assertj.core.api.Assertions.*;

class PaymentServiceTest {
    private final PaymentService payments = new PaymentService();
    @AfterEach void resetLocale() { LocaleContextHolder.resetLocaleContext(); }
    private PaymentService.Request card(String brand, String number, String expiry, String code, String pin) {
        return new PaymentService.Request(brand, "APPROVED", "Demo Customer", number, expiry, code, pin, null, null, null);
    }
    @Test void supportsFourCardNetworksAndReceiptsOnlyContainMaskedMetadata() {
        String[][] examples = {{"VISA","4242424242424242","123"},{"MASTERCARD","5555555555554444","123"},
                {"UNIONPAY","6200000000000005","123"},{"AMEX","378282246310005","1234"}};
        for (var example : examples) {
            var request = card(example[0], example[1], "12/99", example[2], "123456");
            var receipt = payments.pay(BigDecimal.TEN, "test-token", request);
            assertThat(receipt.lastDigits()).isEqualTo(example[1].substring(example[1].length() - 4));
            assertThat(receipt.toString()).doesNotContain(example[1], "123456");
            assertThat(request.toString()).doesNotContain(example[1], "123456");
            assertThat(receipt.reference()).isEqualTo(payments.pay(BigDecimal.TEN, "test-token", request).reference());
        }
    }
    @Test void validatesChecksumBrandExpirySecurityCodeInSelectedLanguage() {
        var invalid = java.util.List.of(card("VISA","4242424242424241","12/99","123","123456"),
                card("AMEX","4242424242424242","12/99","1234","123456"),
                card("VISA","4242424242424242","01/20","123","123456"),
                card("VISA","4242424242424242","12/99","12",null));
        for (var request : invalid) {
            LocaleContextHolder.setLocale(Locale.ENGLISH);
            assertThatThrownBy(() -> payments.pay(BigDecimal.TEN, "t", request)).isInstanceOf(BusinessException.class)
                    .hasMessageMatching("[A-Za-z].*");
            LocaleContextHolder.setLocale(Locale.SIMPLIFIED_CHINESE);
            assertThatThrownBy(() -> payments.pay(BigDecimal.TEN, "t", request)).isInstanceOf(BusinessException.class)
                    .hasMessageMatching(".*[\\p{IsHan}].*");
        }
    }
    @Test void cryptoRequiresMatchingNetworkAndOnlyStoresWalletSuffixAndDemoQuote() {
        var request = new PaymentService.Request("CRYPTO", "APPROVED", null, null, null, null, null,
                "ETH", "ETHEREUM", "0x000000000000000000000000000000000000dEaD");
        var receipt = payments.pay(new BigDecimal("40"), "crypto", request);
        assertThat(receipt.cryptoAmount()).isEqualByComparingTo("0.01000000");
        assertThat(receipt.lastDigits()).isEqualTo("00dEaD");
        assertThat(receipt.toString()).doesNotContain("0x000000");
        var wrong = new PaymentService.Request("CRYPTO", "APPROVED", null, null, null, null, null,
                "BTC", "ETHEREUM", request.cryptoWallet());
        assertThatThrownBy(() -> payments.pay(BigDecimal.TEN, "crypto", wrong)).isInstanceOf(BusinessException.class);
    }
}
