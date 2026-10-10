package sg.edu.nus.iss.shoppingcart.controller;

import jakarta.servlet.http.HttpSession;
import java.util.Map;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import sg.edu.nus.iss.shoppingcart.interceptor.LoginInterceptor;
import sg.edu.nus.iss.shoppingcart.service.AccountFormTokens;
import sg.edu.nus.iss.shoppingcart.service.AvatarService;

@RestController
@RequestMapping("/api/account/avatar")
public class AvatarController {
    private final AvatarService avatars;
    private final MessageSource messages;
    public AvatarController(AvatarService avatars, MessageSource messages) { this.avatars = avatars; this.messages = messages; }

    @GetMapping
    public ResponseEntity<byte[]> read(HttpSession session) {
        return avatars.read(LoginInterceptor.currentUserId(session))
                .map(bytes -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_JPEG)
                        .header("X-Content-Type-Options", "nosniff").body(bytes))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, String> upload(@RequestParam MultipartFile avatar, @RequestParam String accountFormToken, HttpSession session) {
        synchronized (session) {
            AccountFormTokens.require(session, accountFormToken);
            avatars.save(LoginInterceptor.currentUserId(session), avatar);
        }
        return Map.of("url", "/api/account/avatar", "message", messages.getMessage("avatar.saved", null, LocaleContextHolder.getLocale()));
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> tooLarge() {
        return ResponseEntity.badRequest().body(Map.of("message", messages.getMessage("avatar.uploadInvalid", null, LocaleContextHolder.getLocale())));
    }
}
