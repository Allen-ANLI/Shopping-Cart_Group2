package sg.edu.nus.iss.shoppingcart.controller;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sg.edu.nus.iss.shoppingcart.exception.*;
import java.util.Map;

/** All store JSON endpoints share predictable status codes and never return HTML errors. */
@Order(-10)
@RestControllerAdvice(annotations = RestController.class)
public class StoreRestErrorAdvice {
    @ExceptionHandler(NotAuthenticatedException.class)
    public ResponseEntity<?> login(Exception ex) { return error(401, "Please log in to continue"); }
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<?> missing(ResourceNotFoundException ex) { return error(404, ex.getMessage()); }
    @ExceptionHandler({BusinessException.class, IllegalArgumentException.class})
    public ResponseEntity<?> business(Exception ex) { return error(400, ex.getMessage()); }
    @ExceptionHandler({org.springframework.web.bind.MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.HandlerMethodValidationException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    public ResponseEntity<?> invalid(Exception ex) { return error(400, "Please check the information you entered"); }
    private ResponseEntity<?> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("status", status,
                "error", HttpStatus.valueOf(status).getReasonPhrase(), "message", sg.edu.nus.iss.shoppingcart.service.UiText.localize(message)));
    }
}
