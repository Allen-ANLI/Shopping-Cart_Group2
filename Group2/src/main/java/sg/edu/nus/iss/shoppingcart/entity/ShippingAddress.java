package sg.edu.nus.iss.shoppingcart.entity;

import jakarta.persistence.*;

/** An address belongs to one account; orders retain a separate delivery snapshot. */
@Entity
@Table(name = "shipping_addresses", indexes = @Index(name = "idx_address_user", columnList = "user_id"))
public class ShippingAddress {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column(nullable = false, length = 120)
    private String recipientName;
    @Column(nullable = false, length = 30)
    private String phone;
    @Column(nullable = false, length = 80)
    private String country;
    @Column(nullable = false, length = 100)
    private String city;
    @Column(nullable = false, length = 20)
    private String postalCode;
    @Column(nullable = false, length = 200)
    private String addressLine1;
    @Column(length = 200)
    private String addressLine2;
    @Column(nullable = false)
    private boolean defaultAddress;

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User value) { user = value; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String value) { recipientName = value; }
    public String getPhone() { return phone; }
    public void setPhone(String value) { phone = value; }
    public String getCountry() { return country; }
    public void setCountry(String value) { country = value; }
    public String getCity() { return city; }
    public void setCity(String value) { city = value; }
    public String getPostalCode() { return postalCode; }
    public void setPostalCode(String value) { postalCode = value; }
    public String getAddressLine1() { return addressLine1; }
    public void setAddressLine1(String value) { addressLine1 = value; }
    public String getAddressLine2() { return addressLine2; }
    public void setAddressLine2(String value) { addressLine2 = value; }
    public boolean isDefaultAddress() { return defaultAddress; }
    public void setDefaultAddress(boolean value) { defaultAddress = value; }
}
