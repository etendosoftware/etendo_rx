package com.etendorx.auth.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.etendorx.auth.auth.utils.TokenInfo;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class TokenController {

  @Autowired
  private ResourceLoader resourceLoader;

  private static final String AUTH_TOKEN_INFO_URI_WITH_DATE_FORMAT = "/auth/ETRX_Token_Info?_dateFormat=yyyy-MM-dd'T'HH:mm:ss.SSSX";
  @Value("${token}")
  String token;
  @Value("${das.url}")
  String dasUrl;

  @GetMapping("/api/genToken")
  public Object index(HttpServletRequest request, @RequestHeader Map<String, String> headers, @RequestParam(required = false) String userId,
      @RequestParam(required = false) String etrxOauthProviderId) {
    try {
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      DefaultOAuth2User user = (DefaultOAuth2User) authentication.getPrincipal();
      if (StringUtils.equals("FailedTokenCreation", user.getAttribute("name"))) {
        request.setAttribute("errorMessage", "token_failed");
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
      authentication.setAuthenticated(false);


      // Load the HTML template
      Resource resource = resourceLoader.getResource("classpath:templates/oAuthResponse.html");
      String htmlContent = new String(Files.readAllBytes(Paths.get(resource.getURI())));

      // Replace placeholders with actual values
      htmlContent = htmlContent.replace("{{title}}", "Execution Successful")
          .replace("{{titleColor}}", "#4caf50")
          .replace("{{icon}}", "&#10004;")
          .replace("{{iconColor}}", "#4caf50")
          .replace("{{message}}", "Your request has been processed successfully.");

      return htmlContent;


    } catch (ResourceAccessException e1) {
      request.setAttribute("errorMessage", "conn_refuse_das");
      throw new ResourceAccessException("Connection refused with DAS service. Check if the service is Up and Running");
    } catch (IOException e) {
      request.setAttribute("errorMessage", "internal_error");
      throw new RuntimeException(e);
    }
  }
}
