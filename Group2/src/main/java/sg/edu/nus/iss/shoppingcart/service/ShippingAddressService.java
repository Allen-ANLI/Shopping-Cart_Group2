package sg.edu.nus.iss.shoppingcart.service;

import sg.edu.nus.iss.shoppingcart.entity.ShippingAddress;
import sg.edu.nus.iss.shoppingcart.form.ShippingAddressForm;
import java.util.List;

/** Business operations, following the CourseService/Impl tutorial. */
public interface ShippingAddressService {
    List<ShippingAddress> listForUser(Long userId);

    ShippingAddress requireForUser(Long addressId, Long userId);

    ShippingAddress saveForUser(Long userId, Long addressId, ShippingAddressForm form);

    void makeDefault(Long userId, Long addressId);

    void deleteForUser(Long userId, Long addressId);
}
