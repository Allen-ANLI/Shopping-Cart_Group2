package sg.edu.nus.iss.shoppingcart.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * 增量 Session 默认配置，不覆盖 A 的 application.properties/数据库连接。
 * @author luopeiwen
 */
@Configuration
@PropertySource("classpath:auth-session.properties")
public class AuthSessionConfig {
}
