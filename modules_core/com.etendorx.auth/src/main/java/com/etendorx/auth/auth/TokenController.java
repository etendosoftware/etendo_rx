package com.etendorx.auth.auth;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.swing.text.html.HTMLDocument;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
  @Value("${token}")
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
      TokenInfo tokenInfo = new TokenInfo(
          null,
          user.getAttribute("expiresAt").toString(),
          user.getAttribute("token").toString(),
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
