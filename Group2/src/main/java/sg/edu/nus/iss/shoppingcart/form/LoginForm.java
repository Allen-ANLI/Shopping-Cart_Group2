package sg.edu.nus.iss.shoppingcart.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 沿用 A 的 form 包；用户名可裁剪空格，但密码原样验证。
 * @author luopeiwen (B integration)
 */
public class LoginForm {
    @NotBlank(message = "Username is required")
    @Size(max = 50, message = "Username must not exceed 50 characters")
    private String username;
    @NotBlank(message = "Password is required")
    @Size(max = 72, message = "Password must not exceed 72 characters")
    private String password;

    public String getUsername() { return username; }
    public void setUsername(String value) { username = value == null ? null : value.trim(); }
    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
}
