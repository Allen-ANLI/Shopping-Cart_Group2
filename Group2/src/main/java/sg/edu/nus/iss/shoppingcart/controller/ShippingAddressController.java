package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.form.ShippingAddressForm;
import sg.edu.nus.iss.shoppingcart.interceptor.LoginInterceptor;
import sg.edu.nus.iss.shoppingcart.service.AccountFormTokens;
import sg.edu.nus.iss.shoppingcart.service.AuthService;
import sg.edu.nus.iss.shoppingcart.service.ShippingAddressService;

@Controller
@RequestMapping("/account/addresses")
public class ShippingAddressController {
    private final ShippingAddressService addresses;
    private final AuthService auth;
    private final MessageSource messages;
    public ShippingAddressController(ShippingAddressService addresses, AuthService auth, MessageSource messages) {
        this.addresses = addresses; this.auth = auth; this.messages = messages;
    }
    @InitBinder("addressForm")
    public void bindAddress(WebDataBinder binder) {
        binder.setAllowedFields("recipientName", "phone", "country", "city", "postalCode",
                "addressLine1", "addressLine2", "defaultAddress");
    }
    @GetMapping
    public String list(@RequestParam(required = false) String returnTo, HttpSession session, Model model) {
        model.addAttribute("addresses", addresses.listForUser(LoginInterceptor.currentUserId(session)));
        populateCommon(session, returnTo, model);
        return "account/addresses";
    }
    @GetMapping("/new")
    public String newAddress(@RequestParam(required = false) String returnTo, HttpSession session, Model model) {
        ShippingAddressForm form = new ShippingAddressForm();
        auth.findById(LoginInterceptor.currentUserId(session)).ifPresent(user -> {
            form.setRecipientName(user.getFullName() == null ? user.getDisplayName() : user.getFullName());
            form.setPhone(user.getPhone());
        });
        model.addAttribute("addressForm", form);
        populateForm(null, session, returnTo, model);
        return "account/address-form";
    }
    @GetMapping("/{id}/edit")
    public String editAddress(@PathVariable Long id, @RequestParam(required = false) String returnTo,
                              HttpSession session, Model model) {
        model.addAttribute("addressForm", ShippingAddressForm.from(addresses.requireForUser(id, LoginInterceptor.currentUserId(session))));
        populateForm(id, session, returnTo, model);
        return "account/address-form";
    }
    @PostMapping
    public String create(@Valid @ModelAttribute("addressForm") ShippingAddressForm form, BindingResult errors,
                         @RequestParam(required = false) String returnTo,
                         @RequestParam(required = false) String accountFormToken,
                         HttpSession session, Model model, RedirectAttributes flash) {
        return save(null, form, errors, returnTo, accountFormToken, session, model, flash);
    }
    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("addressForm") ShippingAddressForm form,
                         BindingResult errors, @RequestParam(required = false) String returnTo,
                         @RequestParam(required = false) String accountFormToken,
                         HttpSession session, Model model, RedirectAttributes flash) {
        return save(id, form, errors, returnTo, accountFormToken, session, model, flash);
    }
    private String save(Long id, ShippingAddressForm form, BindingResult errors, String returnTo,
                         String token, HttpSession session, Model model, RedirectAttributes flash) {
        synchronized (session) {
            AccountFormTokens.require(session, token);
            Long userId = LoginInterceptor.currentUserId(session);
            if (id != null) { addresses.requireForUser(id, userId); }
            if (!errors.hasErrors()) {
                try {
                    addresses.saveForUser(userId, id, form);
                    flash.addFlashAttribute("successMessage", message("address.saved"));
                    return "redirect:" + safeReturn(returnTo);
                } catch (BusinessException ex) { model.addAttribute("errorMessage", ex.getUserMessage()); }
            }
            populateForm(id, session, returnTo, model);
            return "account/address-form";
        }
    }
    @PostMapping("/{id}/default")
    public String makeDefault(@PathVariable Long id, @RequestParam(required = false) String accountFormToken,
                              @RequestParam(required = false) String returnTo,
                              HttpSession session, RedirectAttributes flash) {
        synchronized (session) {
            AccountFormTokens.require(session, accountFormToken);
            addresses.makeDefault(LoginInterceptor.currentUserId(session), id);
            flash.addFlashAttribute("successMessage", message("address.default.saved"));
            return "redirect:" + listReturn(returnTo);
        }
    }
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, @RequestParam(required = false) String accountFormToken,
                          @RequestParam(required = false) String returnTo,
                          HttpSession session, RedirectAttributes flash) {
        synchronized (session) {
            AccountFormTokens.require(session, accountFormToken);
            addresses.deleteForUser(LoginInterceptor.currentUserId(session), id);
            flash.addFlashAttribute("successMessage", message("address.deleted"));
            return "redirect:" + listReturn(returnTo);
        }
    }
    private void populateCommon(HttpSession session, String returnTo, Model model) {
        model.addAttribute("accountFormToken", AccountFormTokens.get(session));
        model.addAttribute("returnTo", safeReturn(returnTo));
        model.addAttribute("fromCheckout", "/checkout".equals(safeReturn(returnTo)));
    }
    private void populateForm(Long id, HttpSession session, String returnTo, Model model) {
        populateCommon(session, returnTo, model);
        model.addAttribute("addressId", id);
    }
    private String safeReturn(String returnTo) { return "/checkout".equals(returnTo) ? "/checkout" : "/account/addresses"; }
    private String listReturn(String returnTo) {
        return "/checkout".equals(safeReturn(returnTo)) ? "/account/addresses?returnTo=/checkout" : "/account/addresses";
    }
    private String message(String key) { return messages.getMessage(key, null, LocaleContextHolder.getLocale()); }
}
