package sg.edu.nus.iss.shoppingcart.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import sg.edu.nus.iss.shoppingcart.interceptor.AdminAccessInterceptor;
import sg.edu.nus.iss.shoppingcart.interceptor.LoginInterceptor;

/**
 * B/E 共用一份配置：先核对登录身份，再判断管理员权限。
 * @author 蔡千一 (Module E original admin registration)
 * @author luopeiwen (B integration)
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final LoginInterceptor loginInterceptor;
    private final AdminAccessInterceptor adminAccessInterceptor;

    public WebConfig(LoginInterceptor loginInterceptor, AdminAccessInterceptor adminAccessInterceptor) {
        this.loginInterceptor = loginInterceptor;
        this.adminAccessInterceptor = adminAccessInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor).order(0)
                .addPathPatterns("/cart", "/cart/**", "/checkout", "/checkout/**",
                        "/orders", "/orders/**", "/account", "/account/**", "/admin", "/admin/**",
                        "/api/cart", "/api/cart/**", "/api/checkout", "/api/checkout/**",
                        "/api/orders", "/api/orders/**", "/api/account", "/api/account/**",
                        "/api/admin", "/api/admin/**");
        registry.addInterceptor(adminAccessInterceptor).order(1)
                .addPathPatterns("/admin", "/admin/**", "/api/admin", "/api/admin/**");
    }
}
