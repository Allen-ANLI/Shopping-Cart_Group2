package sg.edu.nus.iss.shoppingcart.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;

/**
 * 注册只绑定普通用户资料，不接受 ID、角色或密码哈希。
 * @author luopeiwen
 */
public class RegisterForm {
    @NotBlank(message = "Username is required")
    @Size(min = 4, max = 20, message = "Username must be between 4 and 20 characters")
    @Pattern(regexp = "[A-Za-z0-9]+", message = "Use only letters and digits in the username")
    private String username;
    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    private String password;
    @NotBlank(message = "Please confirm your password")
    @Size(max = 50, message = "Confirm password must not exceed 50 characters")
    private String confirmPassword;
    @NotBlank(message = "Display name is required")
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }

    @AssertTrue(message = "Password must not exceed 72 UTF-8 bytes")
    public boolean isPasswordWithinBcryptLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public String getUsername() { return username; }
    public void setUsername(String value) { username = value == null ? null : value.trim(); }
    public String getPassword() { return password; }
    public void setPassword(String value) { password = value; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String value) { confirmPassword = value; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value == null ? null : value.trim(); }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value == null ? null : value.trim(); }
}
