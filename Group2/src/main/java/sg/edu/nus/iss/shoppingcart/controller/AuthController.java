package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import sg.edu.nus.iss.shoppingcart.entity.User;
import sg.edu.nus.iss.shoppingcart.exception.BusinessException;
import sg.edu.nus.iss.shoppingcart.form.LoginForm;
import sg.edu.nus.iss.shoppingcart.form.RegisterForm;
import sg.edu.nus.iss.shoppingcart.interceptor.LoginInterceptor;
import sg.edu.nus.iss.shoppingcart.service.AuthService;

/**
 * 复用 A 的 AuthService/PasswordConfig；登录后为 C/D/E 提供统一 Session。
 * @author luopeiwen
 */
@Controller
public class AuthController {
    private final AuthService authService;
    public AuthController(AuthService authService) { this.authService = authService; }

    @InitBinder("loginForm")
    public void bindLogin(WebDataBinder binder) { binder.setAllowedFields("username", "password"); }
    @InitBinder("registerForm")
    public void bindRegistration(WebDataBinder binder) {
        binder.setAllowedFields("username", "password", "confirmPassword", "displayName", "email");
    }

    @GetMapping("/login")
    public String showLogin(@ModelAttribute("loginForm") LoginForm form,
                            @RequestParam(required = false) String required,
                            @RequestParam(required = false) String registered,
                            @RequestParam(required = false) String loggedOut, Model model) {
        if (required != null) { model.addAttribute("infoMessage", "Please log in to continue"); }
        if (registered != null) { model.addAttribute("successMessage", "Registration successful. Please log in."); }
        if (loggedOut != null) { model.addAttribute("successMessage", "You have been logged out."); }
        return "auth/login";
    }

    @PostMapping("/login")
    public String processLogin(@Valid @ModelAttribute("loginForm") LoginForm form,
                               BindingResult result, HttpServletRequest request, Model model) {
        if (result.hasErrors()) { return "auth/login"; }
        User user = authService.authenticate(form.getUsername(), form.getPassword()).orElse(null);
        if (user == null) {
            model.addAttribute("errorMessage", "Invalid username or password");
            return "auth/login";
        }
        String savedRedirect = null;
        HttpSession oldSession = request.getSession(false);
        if (oldSession != null) {
            // 与 C 的加购和 D 的结账共用旧 Session 锁，防止切换期间迁移旧购物车。
            synchronized (oldSession) {
                try {
                    Object saved = oldSession.getAttribute(LoginInterceptor.REDIRECT_AFTER_LOGIN);
                    savedRedirect = saved instanceof String value ? LoginInterceptor.safeRedirect(value) : null;
                    oldSession.invalidate();
                } catch (IllegalStateException ignored) { }
            }
        }
        LoginInterceptor.establishSession(request.getSession(true), user);
        if (savedRedirect != null && (user.isAdmin() || !savedRedirect.startsWith("/admin"))) {
            return "redirect:" + savedRedirect;
        }
        return user.isAdmin() ? "redirect:/admin/products" : "redirect:/products";
    }

    @GetMapping("/register")
    public String showRegister(@ModelAttribute("registerForm") RegisterForm form) { return "auth/register"; }

    @PostMapping("/register")
    public String processRegister(@Valid @ModelAttribute("registerForm") RegisterForm form,
                                  BindingResult result, Model model) {
        if (result.hasErrors()) { return "auth/register"; }
        try {
            authService.register(form);
        } catch (BusinessException ex) {
            model.addAttribute("errorMessage", ex.getUserMessage());
            return "auth/register";
        } catch (DataIntegrityViolationException ex) {
            // 等 Service 事务回滚后再查询，兼顾两个注册请求的唯一约束竞争。
            if (authService.findByUsername(form.getUsername()).isEmpty()) { throw ex; }
            result.rejectValue("username", "username.duplicate", "This username is already taken.");
            return "auth/register";
        }
        return "redirect:/login?registered";
    }

    @PostMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            synchronized (session) {
                try { session.invalidate(); } catch (IllegalStateException ignored) { }
            }
        }
        // 销毁身份、cart、cartFormToken、checkoutState 和 D 的完成回执状态。
        return "redirect:/login?loggedOut";
    }

    @GetMapping("/forbidden")
    @ResponseStatus(org.springframework.http.HttpStatus.FORBIDDEN)
    public String forbidden() { return "auth/forbidden"; }
}
