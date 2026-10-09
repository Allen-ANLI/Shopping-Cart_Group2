package sg.edu.nus.iss.shoppingcart;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sg.edu.nus.iss.shoppingcart.interceptor.LoginInterceptor;
import static org.assertj.core.api.Assertions.assertThat;

/** 登录回跳路径不能成为开放重定向。
 * @author luopeiwen
 */
class BLoginRedirectTest {
    @ParameterizedTest
    @ValueSource(strings = {"/cart", "/cart/products", "/checkout", "/checkout/success?key=abc",
            "/orders?page=2", "/orders/12", "/account", "/admin/products"})
    void permitsKnownInternalGetPages(String path) {
        assertThat(LoginInterceptor.safeRedirect(path)).isEqualTo(path);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com/", "//example.com/", "/cart/../admin/products",
            "/cart/%2e%2e/admin", "/%2f%2fexample.com", "/account;evil", "/login",
            "/cart/add", "/api/products", "/account#evil", "/account\\evil", "/account\r\nX-Test:evil"})
    void rejectsUnsafeOrPostOnlyTargets(String path) {
        assertThat(LoginInterceptor.safeRedirect(path)).isNull();
    }
}
