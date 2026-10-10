package sg.edu.nus.iss.shoppingcart.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 沿用 A 的 form 包；用户名可裁剪空格，但密码原样验证。
 * @author luopeiwen (B integration)
 */
public class LoginForm {
    @NotBlank(message = "{auth.username.required}")
    @Size(max = 50, message = "{auth.username.max}")
    private String username;
    @NotBlank(message = "{auth.password.required}")
    @Size(max = 72, message = "{auth.password.max}")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String value) { username = value == null ? null : value.trim(); }
    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
}
