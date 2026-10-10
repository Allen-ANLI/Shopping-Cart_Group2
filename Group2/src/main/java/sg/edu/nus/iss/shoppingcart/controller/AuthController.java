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
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

/**
 * 复用 A 的 AuthService/PasswordConfig；登录后为 C/D/E 提供统一 Session。
 * @author luopeiwen
 */
@Controller
public class AuthController {
    private final AuthService authService;
    private final MessageSource messages;
    public AuthController(AuthService authService, MessageSource messages) {
        this.authService = authService; this.messages = messages;
    }

    @InitBinder("loginForm")
    public void bindLogin(WebDataBinder binder) { binder.setAllowedFields("username", "password", "loginMethod"); }
    @InitBinder("registerForm")
    public void bindRegistration(WebDataBinder binder) {
        binder.setAllowedFields("username", "password", "confirmPassword", "displayName", "email", "phone", "birthday");
    }

    @GetMapping("/login")
    public String showLogin(@ModelAttribute("loginForm") LoginForm form,
                            @RequestParam(required = false) String required,
                            @RequestParam(required = false) String registered,
                            @RequestParam(required = false) String loggedOut,
                            @RequestParam(required = false) String returnTo,
                            HttpSession session, Model model) {
        rememberReturnTo(returnTo, session);
        if (required != null) { model.addAttribute("infoMessage", message("auth.login.required")); }
        if (registered != null) { model.addAttribute("successMessage", message("auth.register.success")); }
        if (loggedOut != null) { model.addAttribute("successMessage", message("auth.logout.success")); }
        return "auth/login";
    }

    @PostMapping("/login")
    public String processLogin(@Valid @ModelAttribute("loginForm") LoginForm form,
                               BindingResult result, HttpServletRequest request, Model model) {
        if (result.hasErrors()) { return "auth/login"; }
        User user = authService.authenticate(form.getLoginMethod(), form.getUsername(), form.getPassword()).orElse(null);
        if (user == null) {
            model.addAttribute("errorMessage", message("auth.login.invalid"));
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
        return "redirect:/products";
    }

    @GetMapping("/register")
    public String showRegister(@ModelAttribute("registerForm") RegisterForm form,
                               @RequestParam(required = false) String returnTo, HttpSession session) {
        rememberReturnTo(returnTo, session);
        return "auth/register";
    }

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
            result.rejectValue("username", "auth.username.duplicate", message("auth.username.duplicate"));
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

    private void rememberReturnTo(String returnTo, HttpSession session) {
        String safe = LoginInterceptor.safeRedirect(returnTo);
        if (safe != null) { session.setAttribute(LoginInterceptor.REDIRECT_AFTER_LOGIN, safe); }
    }
    private String message(String key) {
        return messages.getMessage(key, null, LocaleContextHolder.getLocale());
    }
}
