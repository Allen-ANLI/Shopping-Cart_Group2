package sg.edu.nus.iss.shoppingcart.service;

import org.springframework.stereotype.Service;
import org.springframework.context.i18n.LocaleContextHolder;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.Set;
import java.util.UUID;

/** Local gateway simulation. Sensitive input exists only for validation and is never persisted. */
@Service
public class PaymentService {
    public record Request(String method, String outcome, String cardholderName, String cardNumber,
                          String cardExpiry, String cardSecurityCode, String paymentPin,
                          String cryptoAsset, String cryptoNetwork, String cryptoWallet) {
        // Compatibility for internal callers; browser checkout always submits the full form.
        public Request(String method, String outcome) {
            this("SIMULATED_CARD".equals(method) ? "VISA" : method, outcome, "Demo Customer",
                    "4242424242424242", "12/99", "123", "123456", null, null, null);
        }
        public static Request success() { return new Request("VISA", "APPROVED"); }
        @Override public String toString() { return "PaymentRequest[redacted]"; }
    }
    public record Receipt(String method, String reference, String instrument, String lastDigits,
                          String cryptoAsset, String cryptoNetwork, BigDecimal cryptoAmount) {}

    public Receipt pay(BigDecimal amount, String checkoutToken, Request request) {
        if (amount == null || amount.signum() < 0) throw error("Invalid payment amount.", "支付金额无效。");
        if (request == null || !Set.of("VISA", "MASTERCARD", "UNIONPAY", "AMEX", "CRYPTO").contains(safe(request.method()))
                || !Set.of("APPROVED", "DECLINED").contains(safe(request.outcome()))) {
            throw error("Select a valid payment method and simulation result.", "请选择有效的支付方式和模拟结果。");
        }
        String instrument;
        String digits;
        BigDecimal cryptoAmount = null;
        String asset = null, network = null;
        if ("CRYPTO".equals(request.method())) {
            asset = safe(request.cryptoAsset()); network = safe(request.cryptoNetwork());
            if (!("BTC".equals(asset) && "BITCOIN".equals(network)
                    || "ETH".equals(asset) && "ETHEREUM".equals(network)
                    || "USDT".equals(asset) && Set.of("ETHEREUM", "TRON").contains(network))) {
                throw error("Choose a compatible cryptocurrency and network.", "请选择匹配的加密货币与网络。");
            }
            String wallet = safe(request.cryptoWallet());
            boolean valid = "ETHEREUM".equals(network) ? wallet.matches("0x[0-9a-fA-F]{40}")
                    : "TRON".equals(network) ? wallet.matches("T[1-9A-HJ-NP-Za-km-z]{33}")
                    : wallet.matches("(?:bc1[ac-hj-np-z02-9]{25,62}|[13][1-9A-HJ-NP-Za-km-z]{25,34})");
            if (!valid) throw error("Enter a valid demo wallet address for the selected network.", "请输入与所选网络匹配的模拟钱包地址。");
            instrument = asset; digits = wallet.substring(wallet.length() - 6);
            // Fixed demonstration quotes in SGD, deliberately not live market prices.
            BigDecimal quote = new BigDecimal("BTC".equals(asset) ? "100000" : "ETH".equals(asset) ? "4000" : "1.30");
            cryptoAmount = amount.divide(quote, 8, RoundingMode.HALF_UP);
        } else {
            String number = safe(request.cardNumber()).replace(" ", "").replace("-", "");
            if (!safe(request.cardholderName()).matches("[\\p{L} .'-]{2,80}"))
                throw error("Enter the cardholder name (2–80 letters).", "请输入持卡人姓名（2–80 个字母或汉字）。");
            if (!number.matches("[0-9]{13,19}") || !luhn(number) || !matchesBrand(number, request.method()))
                throw error("Enter a valid card number matching the selected card network.", "请输入与所选卡组织匹配的有效卡号。");
            if (!safe(request.cardExpiry()).matches("(0[1-9]|1[0-2])/[0-9]{2}"))
                throw error("Enter the expiry date as MM/YY.", "请以 MM/YY 格式填写有效期。");
            String[] expiry = request.cardExpiry().split("/");
            if (YearMonth.of(2000 + Integer.parseInt(expiry[1]), Integer.parseInt(expiry[0])).isBefore(YearMonth.now()))
                throw error("This card has expired. Enter a future expiry date.", "银行卡已过期，请填写有效的到期日期。");
            int securityLength = "AMEX".equals(request.method()) ? 4 : 3;
            if (!safe(request.cardSecurityCode()).matches("[0-9]{" + securityLength + "}"))
                throw error("Enter the " + securityLength + "-digit card security code.", "请输入 " + securityLength + " 位银行卡安全码。");

            instrument = switch (request.method()) {
                case "MASTERCARD" -> "Mastercard"; case "UNIONPAY" -> "UnionPay";
                case "AMEX" -> "American Express"; default -> "Visa";
            };
            digits = number.substring(number.length() - 4);
        }
        if ("DECLINED".equals(request.outcome()))
            throw error("Payment declined. Check the simulation result and try again.", "模拟支付被拒绝，请调整模拟结果后重试。");
        return new Receipt(request.method(), "TXN-" + UUID.nameUUIDFromBytes(checkoutToken.getBytes(StandardCharsets.UTF_8)),
                instrument, digits, asset, network, cryptoAmount);
    }

    private static boolean matchesBrand(String number, String brand) {
        return switch (brand) {
            case "VISA" -> number.startsWith("4") && Set.of(13, 16, 19).contains(number.length());
            case "AMEX" -> number.matches("3[47][0-9]{13}");
            case "UNIONPAY" -> number.startsWith("62") && number.length() >= 16;
            case "MASTERCARD" -> number.length() == 16 && (number.matches("5[1-5][0-9]{14}")
                    || Integer.parseInt(number.substring(0, 4)) >= 2221 && Integer.parseInt(number.substring(0, 4)) <= 2720);
            default -> false;
        };
    }
    private static boolean luhn(String number) {
        int sum = 0; boolean doubleDigit = false;
        for (int i = number.length() - 1; i >= 0; i--) {
            int n = number.charAt(i) - '0';
            if (doubleDigit) { n *= 2; if (n > 9) n -= 9; }
            sum += n; doubleDigit = !doubleDigit;
        }
        return sum % 10 == 0;
    }
    private static String safe(String value) { return value == null ? "" : value.trim(); }
    private static BusinessException error(String en, String zh) { return new BusinessException(localized(en, zh)); }
    public static String localized(String en, String zh) {
        return "zh".equals(LocaleContextHolder.getLocale().getLanguage()) ? zh : en;
    }
}
