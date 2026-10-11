package sg.edu.nus.iss.shoppingcart.form;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.nio.charset.StandardCharsets;

/** Password changes require the current password and the registration password policy. */
public class PasswordChangeForm {
    @NotBlank(message = "{account.password.current.required}")
    @Size(max = 72, message = "{auth.password.max}")
    private String currentPassword;

    @NotBlank(message = "{account.password.new.required}")
    @Size(min = 8, max = 50, message = "{auth.password.length}")
    @Pattern(regexp = "(?s)(?=.*[A-Z])(?=.*[a-z])(?=.*[\\p{P}\\p{S}]).+", message = "{auth.password.strength}")
    private String newPassword;

    @NotBlank(message = "{auth.password.confirm.required}")
    @Size(max = 50, message = "{auth.password.confirm.length}")
    private String confirmPassword;

    @AssertTrue(message = "{auth.password.mismatch}")
    public boolean isPasswordConfirmed() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }

    @AssertTrue(message = "{auth.password.bytes}")
    public boolean isPasswordWithinBcryptLimit() {
        return withinBcryptLimit(currentPassword) && withinBcryptLimit(newPassword);
    }

    private static boolean withinBcryptLimit(String value) {
        return value == null || value.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    public String getCurrentPassword() { return currentPassword; }
    public void setCurrentPassword(String value) { currentPassword = value; }
    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String value) { newPassword = value; }
    public String getConfirmPassword() { return confirmPassword; }
    public void setConfirmPassword(String value) { confirmPassword = value; }
}
