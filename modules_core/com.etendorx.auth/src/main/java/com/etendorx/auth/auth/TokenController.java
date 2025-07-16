package com.etendorx.auth.auth;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
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

  private static final String ERROR_MESSAGE = "errorMessage";
  @Autowired
  private ResourceLoader resourceLoader;

  private static final String AUTH_TOKEN_INFO_URI_WITH_DATE_FORMAT = "/auth/ETRX_Token_Info?_dateFormat=yyyy-MM-dd'T'HH:mm:ss.SSSX";
  private static final DateTimeFormatter EXPIRES_AT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.nnnnnnnnnX")
      .withZone(ZoneOffset.UTC);

  @Value("${admin.token}")
  String token;
  @Value("${das.url}")
  String dasUrl;

  @GetMapping("/api/genToken")
  public Object index(HttpServletRequest request, @RequestParam(required = false) String userId,
      @RequestParam(required = false) String etrxOauthProviderId) {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      DefaultOAuth2User user = (DefaultOAuth2User) authentication.getPrincipal();
      if (StringUtils.equals("FailedTokenCreation", user.getAttribute("name"))) {
        request.setAttribute(ERROR_MESSAGE, "token_failed");
        throw new IllegalArgumentException("Token creation failed! Try again later. " +
            "If the problem persists, please contact your system administrator.");
      }
      String uuid = UUID.randomUUID().toString().toUpperCase().replace("-", "");
      String expiresAt = Optional.ofNullable(user.getAttribute("expiresAt"))
          .map(Object::toString)
          .filter(StringUtils::isNotBlank)
          .orElseGet(() -> {
            Instant idTokenExpiresAt = ((DefaultOidcUser) user).getIdToken().getExpiresAt();
            assert idTokenExpiresAt != null : "";
            return idTokenExpiresAt.toString();
          });

      String tokenValue = Optional.ofNullable(user.getAttribute("token"))
          .map(Object::toString)
          .filter(StringUtils::isNotBlank)
          .orElseGet(() -> {
            String idTokenValue = ((DefaultOidcUser) user).getIdToken().getTokenValue();
            assert idTokenValue != null : "";
            return idTokenValue;
          });

      TokenInfo tokenInfo = new TokenInfo(
          uuid,
          parseAndFormatExpiresAt(expiresAt),
          tokenValue,
          userId,
          etrxOauthProviderId
      );
      HttpHeaders tokenHeaders = new HttpHeaders();
      tokenHeaders.set("Authorization", "Bearer " + token);
      // check if exists
      new RestTemplate().exchange(dasUrl + AUTH_TOKEN_INFO_URI_WITH_DATE_FORMAT,
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
   * Parses a date string into an Instant and returns it as a string
   * in ISO-8601 format with nanoseconds and 'Z' timezone.
   *
   * @param expiresAtString the date string to parse
   * @return the formatted date string, or null if invalid
   */
  public static String parseAndFormatExpiresAt(String expiresAtString) {
    try {
      Instant instant = Instant.parse(expiresAtString);
      return EXPIRES_AT_FORMATTER.format(instant);
    } catch (DateTimeParseException | NullPointerException e) {
      log.error("Invalid date: " + expiresAtString);
      return null;
    }
  }

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
