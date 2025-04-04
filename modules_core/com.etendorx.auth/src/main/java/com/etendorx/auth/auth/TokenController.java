package com.etendorx.auth.auth;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.etendorx.auth.auth.utils.TokenInfo;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
public class TokenController {

  private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX")
      .withZone(ZoneOffset.UTC);

  private static final String ERROR_MESSAGE = "errorMessage";
  @Autowired
  private ResourceLoader resourceLoader;

  private static final String AUTH_TOKEN_INFO_URI = "/auth/ETRX_Token_Info";
  @Value("${adminToken}")
  String token;
  @Value("${das.url}")
  String dasUrl;

  @GetMapping("/api/genToken")
  public Object index(HttpServletRequest request, @RequestParam(required = false) String userId,
      @RequestParam(required = false) String etrxOauthProviderId) {
    try {
      log.info("userId from TokenController : " + userId);
      log.info("etrxOauthProviderId from TokenController : " + etrxOauthProviderId);
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      DefaultOAuth2User user = (DefaultOAuth2User) authentication.getPrincipal();
      if (StringUtils.equals("FailedTokenCreation", user.getAttribute("name"))) {
        request.setAttribute(ERROR_MESSAGE, "token_failed");
        throw new IllegalArgumentException("Token creation failed! Try again later. " +
            "If the problem persists, please contact your system administrator.");
      }

      final String expireAt = getStrExpirationTime(user);
      String tokenValue = !StringUtils.isBlank(user.getAttribute("token")) ?
          user.getAttribute("token").toString() : ((DefaultOidcUser) user).getIdToken().getTokenValue();
      String uuid = UUID.randomUUID().toString().toUpperCase().replace("-", "");

      TokenInfo tokenInfo = new TokenInfo(
          uuid,
          expireAt,
          tokenValue,
          userId,
          etrxOauthProviderId
      );
      HttpHeaders tokenHeaders = new HttpHeaders();
      tokenHeaders.set("Authorization", "Bearer " + token);
      new RestTemplate().exchange(dasUrl + AUTH_TOKEN_INFO_URI,
          HttpMethod.POST,
          new HttpEntity<>(tokenInfo, tokenHeaders),
          TokenInfo.class);
      return generateHtml("Execution Successful", "#4caf50", "&#10004;", "#4caf50", "Your request has been processed successfully.");
    } catch (ResourceAccessException e1) {
      log.error(e1.getMessage(), e1);
      request.setAttribute(ERROR_MESSAGE, "conn_refuse_das");
      throw new ResourceAccessException("Connection refused with DAS service. Check if the service is Up and Running");
    } catch (Exception e) {
      log.error(e.getMessage(), e);
      request.setAttribute(ERROR_MESSAGE, "internal_error");
      throw new RuntimeException("An unexpected error occurred: " + e.getMessage(), e);
    }
  }

  /**
   * This method retrieves the expiration time from the user attributes.
   * It first checks for "expiresAt", then "exp", and finally parses the expiration time.
   *
   * @param user the authenticated user
   * @return the formatted expiration time as a string
   */
  private static String getStrExpirationTime(DefaultOAuth2User user) {
    return Optional.ofNullable(user.getAttribute("expiresAt"))
        .or(() -> Optional.ofNullable(user.getAttribute("exp")))
        .map(exp -> parseExpiration(exp.toString()))
        .orElse("");
  }

    /**
     * This method parses the expiration time from a string.
     * It first tries to parse it as a long epoch second, then as a LocalDateTime.
     * If both fail, it returns the original string.
     *
     * @param exp the expiration time as a string
     * @return the formatted expiration time as a string
     */
  private static String parseExpiration(String exp) {
    try {
      Instant instant = Instant.ofEpochSecond(Long.parseLong(exp));
      return FORMATTER.format(instant);
    } catch (NumberFormatException e) {
      try {
        LocalDateTime dateTime = LocalDateTime.parse(exp);
        return FORMATTER.format(dateTime.toInstant(ZoneOffset.UTC));
      } catch (Exception ex) {
        return exp;
      }
    }
  }

    /**
     * This method generates an HTML response using a template.
     * It reads the HTML template from the classpath, replaces placeholders with actual values,
     * and returns the generated HTML as a string.
     *
     * @param title the title to be displayed
     * @param titleColor the color of the title
     * @param icon the icon to be displayed
     * @param iconColor the color of the icon
     * @param message the message to be displayed
     * @return the generated HTML as a string
     */
  public String generateHtml(String title, String titleColor, String icon, String iconColor, String message) {
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("templates/oAuthResponse.html")) {
      if (inputStream == null) {
        throw new RuntimeException("Resource not found: templates/oAuthResponse.html");
      }
      String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      html = html.replace("{{title}}", title)
          .replace("{{titleColor}}", titleColor)
          .replace("{{icon}}", icon)
          .replace("{{iconColor}}", iconColor)
          .replace("{{message}}", message);
      return html;
    } catch (IOException e) {
      log.error(e.getMessage(), e);
      throw new RuntimeException("Error reading HTML template", e);
    }
  }
}
