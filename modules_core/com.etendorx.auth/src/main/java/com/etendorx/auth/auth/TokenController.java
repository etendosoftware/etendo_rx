package com.etendorx.auth.auth;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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

    RestTemplate restTemp = new RestTemplate();
    HttpHeaders tokenHeaders = new HttpHeaders();
    tokenHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    tokenHeaders.set("Authorization", "Bearer " + token);
    TokenInfo tokenInfo = new TokenInfo(
        null,
        user.getAttribute("expiresAt").toString(),
        user.getAttribute("token").toString(),
        userId,
        etrxOauthProviderId
    );
    // Entity to send
    HttpEntity<TokenInfo> httpEntity = new HttpEntity<>(tokenInfo, tokenHeaders);
    // check if exists
    restTemp.exchange(dasUrl + "/auth/ETRX_Token_Info?_dateFormat=yyyy-MM-dd'T'HH:mm:ss.SSSX",
        HttpMethod.POST,
        httpEntity, TokenInfo.class);
    return "Token Created! You can close this window";
  }
}
