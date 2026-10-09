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

/**
 * 资料修改仅取当前 Session ID，不接受请求中指定的用户、角色或哈希。
 * @author luopeiwen
 */
@Controller
public class AccountController {
    private final AuthService authService;
    public AccountController(AuthService authService) { this.authService = authService; }

    @InitBinder("profileForm")
    public void bindProfile(WebDataBinder binder) { binder.setAllowedFields("displayName", "email"); }

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
                                RedirectAttributes flash) {
        synchronized (session) {
            Long userId = LoginInterceptor.currentUserId(session);
            if (userId == null) { return "redirect:/login?required"; }
            if (result.hasErrors()) {
                populate(session, model);
                return "account/view";
            }
            authService.updateProfile(userId, form);
            flash.addFlashAttribute("successMessage", "Your profile has been updated.");
            return "redirect:/account";
        }
    }

    private void populate(HttpSession session, Model model) {
        Long userId = LoginInterceptor.currentUserId(session);
        User user = authService.findById(userId).orElseThrow(
                () -> new IllegalArgumentException("The account is no longer available"));
        model.addAttribute("account", AuthenticatedUser.from(user));
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", new ProfileForm(user.getDisplayName(), user.getEmail()));
        }
    }
}
