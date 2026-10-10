package sg.edu.nus.iss.shoppingcart.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import jakarta.validation.constraints.Past;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 注册只绑定普通用户资料，不接受 ID、角色或密码哈希。
 * @author luopeiwen
 */
public class RegisterForm {
    @NotBlank(message = "{auth.username.required}")
    @Size(min = 4, max = 20, message = "{auth.username.length}")
    @Pattern(regexp = "[A-Za-z0-9]+", message = "{auth.username.invalid}")
    private String username;
    @NotBlank(message = "{auth.password.required}")
    @Size(min = 8, max = 50, message = "{auth.password.length}")
    @Pattern(regexp = "(?s)(?=.*[A-Z])(?=.*[a-z])(?=.*[\\p{P}\\p{S}]).+", message = "{auth.password.strength}")
    private String password;
    @NotBlank(message = "{auth.password.confirm.required}")
    @Size(max = 50, message = "{auth.password.confirm.length}")
    private String confirmPassword;
    @NotBlank(message = "{account.displayName.required}")
    @Size(max = 100, message = "{account.displayName.length}")
    private String displayName;
    @NotBlank(message = "{account.email.required}")
    @Email(message = "{account.email.invalid}")
    @Size(max = 255, message = "{account.email.length}")
    private String email;

    @NotBlank(message = "{account.phone.required}")
    @Pattern(regexp = "(?=(?:[^0-9]*[0-9]){6,15}[^0-9]*$)[+0-9][0-9 ()-]{5,29}", message = "{account.phone.invalid}")
    private String phone;
    @Past(message = "{account.birthday.invalid}")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthday;

    @AssertTrue(message = "{auth.password.mismatch}")
    public boolean isPasswordConfirmed() {
        return password != null && password.equals(confirmPassword);
    }

    @AssertTrue(message = "{auth.password.bytes}")
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
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value == null ? null : value.trim(); }
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate value) { birthday = value; }
}
