package sg.edu.nus.iss.shoppingcart.service;

import jakarta.validation.Validator;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sg.edu.nus.iss.shoppingcart.entity.ShippingAddress;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.form.ShippingAddressForm;
import sg.edu.nus.iss.shoppingcart.repository.ShippingAddressRepository;
import sg.edu.nus.iss.shoppingcart.repository.UserRepository;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ShippingAddressService {
    private final ShippingAddressRepository addresses;
    private final UserRepository users;
    private final Validator validator;
    private final MessageSource messages;
    public ShippingAddressService(ShippingAddressRepository addresses, UserRepository users,
                                  Validator validator, MessageSource messages) {
        this.addresses = addresses; this.users = users;
        this.validator = validator; this.messages = messages;
    }
    public List<ShippingAddress> listForUser(Long userId) {
        return addresses.findByUser_IdOrderByDefaultAddressDescIdAsc(userId);
    }
    public ShippingAddress requireForUser(Long addressId, Long userId) {
        if (addressId == null || userId == null) { throw notFound(); }
        return addresses.findByIdAndUser_Id(addressId, userId).orElseThrow(this::notFound);
    }
    @Transactional
    public ShippingAddress saveForUser(Long userId, Long addressId, ShippingAddressForm form) {
        var errors = validator.validate(form);
        if (!errors.isEmpty()) { throw new BusinessException(errors.iterator().next().getMessage()); }
        // Serialize address changes for the same account, including across sessions.
        User owner = users.lockById(userId).orElseThrow(this::notFound);
        List<ShippingAddress> existing = listForUser(userId);
        if (addressId == null && existing.size() >= 20) {
            throw new BusinessException(message("address.limit"));
        }
        ShippingAddress address = addressId == null ? new ShippingAddress() : requireForUser(addressId, userId);
        address.setUser(owner);
        form.applyTo(address);
        if (form.isDefaultAddress() || existing.isEmpty()) {
            existing.forEach(item -> item.setDefaultAddress(false));
            address.setDefaultAddress(true);
        }
        return addresses.saveAndFlush(address);
    }
    @Transactional
    public void makeDefault(Long userId, Long addressId) {
        users.lockById(userId).orElseThrow(this::notFound);
        ShippingAddress selected = requireForUser(addressId, userId);
        listForUser(userId).forEach(item -> item.setDefaultAddress(item.getId().equals(selected.getId())));
    }
    @Transactional
    public void deleteForUser(Long userId, Long addressId) {
        users.lockById(userId).orElseThrow(this::notFound);
        ShippingAddress selected = requireForUser(addressId, userId);
        boolean wasDefault = selected.isDefaultAddress();
        addresses.delete(selected);
        addresses.flush();
        if (wasDefault) { listForUser(userId).stream().findFirst().ifPresent(item -> item.setDefaultAddress(true)); }
    }
    private String message(String key) { return messages.getMessage(key, null, LocaleContextHolder.getLocale()); }
    private ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, message("address.notFound")); }
}
