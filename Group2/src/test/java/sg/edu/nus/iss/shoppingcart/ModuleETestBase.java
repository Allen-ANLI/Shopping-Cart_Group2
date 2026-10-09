package sg.edu.nus.iss.shoppingcart;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/**
 * 测试类公共基类。
 *
 * <p>统一用 H2 内存库跑测试：任何一台机器 clone 下来就能执行
 * {@code mvn test}，不需要先装MySQL 或手动建库。
 * 正式运行仍然走 MySQL。</p>
 *
 * <p>{@code @Transactional} 让每个测试方法结束后自动回滚，
 * 测试之间不会互相污染——比如一个测试造了订单，
 * 不会让另一个测试的历史列表凭空多出数据。</p>
 *
 * @author 蔡千一（Module E）
 */
@SpringBootTest
@ActiveProfiles("h2")
@Transactional
public abstract class ModuleETestBase {
}