package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sg.edu.nus.iss.shoppingcart.dto.AuthenticatedUser;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.form.ProfileForm;
import sg.edu.nus.iss.shoppingcart.interceptor.LoginInterceptor;
import sg.edu.nus.iss.shoppingcart.service.AuthService;
import sg.edu.nus.iss.shoppingcart.service.AccountFormTokens;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 资料修改仅取当前 Session ID，不接受请求中指定的用户、角色或哈希。
 * @author luopeiwen
 */
@Controller
public class AccountController {
    private final AuthService authService;
    private final MessageSource messages;
    private final sg.edu.nus.iss.shoppingcart.service.AvatarService avatars;
    public AccountController(AuthService authService, MessageSource messages, sg.edu.nus.iss.shoppingcart.service.AvatarService avatars) {
        this.authService = authService; this.messages = messages; this.avatars = avatars;
    }

    @InitBinder("profileForm")
    public void bindProfile(WebDataBinder binder) {
        binder.setAllowedFields("displayName", "email", "phone", "birthday");
    }

    @GetMapping("/account")
    public String viewAccount(HttpSession session, Model model) {
        synchronized (session) {
            populate(session, model);
            return "account/view";
        }
    }

    @PostMapping("/account/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult result, HttpSession session, Model model,
                                RedirectAttributes flash,
                                @RequestParam(required = false) String accountFormToken) {
        synchronized (session) {
            Long userId = LoginInterceptor.currentUserId(session);
            if (userId == null) { return "redirect:/login?required"; }
            AccountFormTokens.require(session, accountFormToken);
            if (result.hasErrors()) {
                populate(session, model);
                return "account/view";
            }
            try {
                authService.updateProfile(userId, form);
            } catch (BusinessException ex) {
                model.addAttribute("errorMessage", ex.getUserMessage());
                populate(session, model);
                return "account/view";
            }
            flash.addFlashAttribute("successMessage", messages.getMessage("account.saved", null, LocaleContextHolder.getLocale()));
            return "redirect:/account";
        }
    }

    private void populate(HttpSession session, Model model) {
        Long userId = LoginInterceptor.currentUserId(session);
        User user = authService.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("The account is no longer available"));
        model.addAttribute("account", AuthenticatedUser.from(user));
        model.addAttribute("hasAvatar", avatars.exists(userId));
        model.addAttribute("accountFormToken", AccountFormTokens.get(session));
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", ProfileForm.from(user));
        }
    }
}
