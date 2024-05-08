package com.etendorx.auth.auth;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.client.RestTemplate;
import com.etendorx.auth.auth.utils.TokenInfo;

@RestController("/")
public class TokenController {
  private static final String CENTERED_DIV = "<div style=\"display: flex;align-items: center; justify-content: center; text-align: center;\">";
  private static final String CLOSED_DIV = "</div>";
  @Value("${auth.token:}")
  String token;
  @Value("${das.url:}")
  String dasUrl;

  @GetMapping("/api/genToken")
  public String index(@RequestHeader Map<String, String> headers, @RequestParam String userId,
      @RequestParam String etrxOauthProviderId) {
    Authentication authentication = SecurityContextHolder.getContext()
        .getAuthentication();
    DefaultOAuth2User user = (DefaultOAuth2User) authentication.getPrincipal();
    if (StringUtils.equals("FailedTokenCreation", user.getAttribute("name"))) {
      return CENTERED_DIV + "Token creation failed!" + CLOSED_DIV +
          CENTERED_DIV + " Try again later. If the problem persist, please, contact your system administrator." + CLOSED_DIV;
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
    new RestTemplate().exchange(dasUrl + "/auth/ETRX_Token_Info?_dateFormat=yyyy-MM-dd'T'HH:mm:ss.SSSX",
        HttpMethod.POST,
        new HttpEntity<>(tokenInfo, tokenHeaders),
        TokenInfo.class);
    return CENTERED_DIV + "Token Created!" + CLOSED_DIV +
        CENTERED_DIV + "You can close this window." + CLOSED_DIV;
  }
}
