package sg.edu.nus.iss.shoppingcart.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import sg.edu.nus.iss.shoppingcart.entity.User;

/**
 * 只能修改当前登录用户的展示名与邮箱。
 * @author luopeiwen
 */
public class ProfileForm {
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

    public ProfileForm() {}
    public ProfileForm(String displayName, String email) {
        setDisplayName(displayName);
        setEmail(email);
    }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String value) { displayName = value == null ? null : value.trim(); }
    public String getEmail() { return email; }
    public void setEmail(String value) { email = value == null ? null : value.trim(); }
    public static ProfileForm from(User user) {
        ProfileForm form = new ProfileForm(user.getDisplayName(), user.getEmail());
        form.setPhone(user.getPhone());
        form.setBirthday(user.getBirthday());
        return form;
    }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value == null ? null : value.trim(); }
    public LocalDate getBirthday() { return birthday; }
    public void setBirthday(LocalDate value) { birthday = value; }
}
