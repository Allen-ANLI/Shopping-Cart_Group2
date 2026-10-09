package sg.edu.nus.iss.shoppingcart.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 只能修改当前登录用户的展示名与邮箱。
 * @author luopeiwen
 */
public class ProfileForm {
    @NotBlank(message = "Display name is required")
    @Size(max = 100, message = "Display name must not exceed 100 characters")
    private String displayName;
    @NotBlank(message = "Email is required")
    @Email(message = "Please enter a valid email address")
    @Size(max = 255, message = "Email must not exceed 255 characters")
    private String email;

    public ProfileForm() {}
    public ProfileForm(String displayName, String email) {
        setDisplayName(displayName);
        setEmail(email);
    }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value == null ? null : value.trim(); }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value == null ? null : value.trim(); }
}
