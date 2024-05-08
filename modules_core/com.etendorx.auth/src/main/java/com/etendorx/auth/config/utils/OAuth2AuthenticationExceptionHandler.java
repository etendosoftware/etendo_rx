import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

@ControllerAdvice
public class OAuth2AuthenticationExceptionHandler {
  @ExceptionHandler(OAuth2AuthenticationException.class)
  public ResponseEntity<String> handleOAuth2AuthenticationException(OAuth2AuthenticationException e) {
    // Return a structured error response
    return ResponseEntity.status(401).body("Authentication Error: " + e.getMessage());
  }
}