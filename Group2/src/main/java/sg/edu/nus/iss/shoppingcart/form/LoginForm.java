package sg.edu.nus.iss.shoppingcart.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

/**
 * 沿用 A 的 form 包；用户名可裁剪空格，但密码原样验证。
 * @author luopeiwen (B integration)
 */
public class LoginForm {
    @NotBlank(message = "{auth.identifier.required}")
    @Size(max = 255, message = "{auth.identifier.max}")
    private String username;
    @NotBlank(message = "{auth.method.invalid}")
    @Pattern(regexp = "username|email|phone", message = "{auth.method.invalid}")
    private String loginMethod = "username";
    @NotBlank(message = "{auth.password.required}")
    @Size(max = 72, message = "{auth.password.max}")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String value) { username = value == null ? null : value.trim(); }
    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
    public String getLoginMethod() { return loginMethod; }
    public void setLoginMethod(String value) { loginMethod = value; }
}
