package sg.edu.nus.iss.shoppingcart.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import sg.edu.nus.iss.shoppingcart.entity.ShippingAddress;

public class ShippingAddressForm {
    @NotBlank(message = "{address.recipient.required}")
    @Size(max = 120, message = "{account.fullName.length}")
    private String recipientName;
    @NotBlank(message = "{account.phone.required}")
    @Pattern(regexp = "[+0-9][0-9 ()-]{5,29}", message = "{account.phone.invalid}")
    private String phone;
    @NotBlank(message = "{address.country.required}") @Size(max = 80, message = "{address.field.tooLong}")
    private String country;
    @NotBlank(message = "{address.city.required}") @Size(max = 100, message = "{address.field.tooLong}")
    private String city;
    @NotBlank(message = "{address.postal.required}") @Size(max = 20, message = "{address.field.tooLong}")
    @Pattern(regexp = "[\\p{L}\\p{N} -]+", message = "{address.postal.invalid}")
    private String postalCode;
    @NotBlank(message = "{address.line1.required}") @Size(max = 200, message = "{address.field.tooLong}")
    private String addressLine1;
    @Size(max = 200, message = "{address.field.tooLong}")
    private String addressLine2;
    private boolean defaultAddress;

    public static ShippingAddressForm from(ShippingAddress address) {
        ShippingAddressForm form = new ShippingAddressForm();
        form.setRecipientName(address.getRecipientName());
        form.setPhone(address.getPhone()); form.setCountry(address.getCountry());
        form.setCity(address.getCity()); form.setPostalCode(address.getPostalCode());
        form.setAddressLine1(address.getAddressLine1()); form.setAddressLine2(address.getAddressLine2());
        form.setDefaultAddress(address.isDefaultAddress());
        return form;
    }
    public void applyTo(ShippingAddress address) {
        address.setRecipientName(recipientName); address.setPhone(phone);
        address.setCountry(country); address.setCity(city); address.setPostalCode(postalCode);
        address.setAddressLine1(addressLine1); address.setAddressLine2(addressLine2);
    }
    private static String trimmed(String value) { return value == null ? null : value.trim(); }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String value) { recipientName = trimmed(value); }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = trimmed(value); }
    public String getCountry() { return country; }
    public void setCountry(String value) { country = trimmed(value); }
    public String getCity() { return city; }
    public void setCity(String value) { city = trimmed(value); }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String value) { postalCode = trimmed(value); }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String value) { addressLine1 = trimmed(value); }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String value) { addressLine2 = trimmed(value); }
    public boolean isDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(boolean value) { defaultAddress = value; }
}
