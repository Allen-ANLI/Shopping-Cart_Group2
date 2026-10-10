package sg.edu.nus.iss.shoppingcart.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Delivery details copied at checkout; editing an address never changes an existing order. */
@Embeddable
public class ShippingSnapshot {
    @Column(name = "shipping_recipient", length = 120)
    private String recipientName;
    @Column(name = "shipping_phone", length = 40)
    private String phone;
    @Column(name = "shipping_address", length = 1000)
    private String fullAddress;

    protected ShippingSnapshot() { }
    public ShippingSnapshot(ShippingAddress address) {
        recipientName = address.getRecipientName();
        phone = address.getPhone();
        fullAddress = Stream.of(address.getAddressLine1(), address.getAddressLine2(),
                address.getCity(), address.getPostalCode(), address.getCountry())
                .filter(value -> value != null && !value.isBlank()).collect(Collectors.joining(", "));
    }
    public String getRecipientName() { return recipientName; }
    public String getPhone() { return phone; }
    public String getFullAddress() { return fullAddress; }
}
