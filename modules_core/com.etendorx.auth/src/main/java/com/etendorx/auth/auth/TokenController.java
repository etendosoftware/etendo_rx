package com.etendorx.auth.auth;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.etendorx.auth.auth.utils.TokenInfo;

@RestController("/")
public class TokenController {
  /*@Value("${auth.token:}")
  String token;*/
  @Value("${das.url:'http://localhost:8092'}")
  String dasUrl;

  @GetMapping("/api/genToken")
  public String index(@RequestHeader Map<String, String> headers, @RequestParam String userId,
      @RequestParam String etrxOauthProviderId) {
    Authentication authentication = SecurityContextHolder.getContext()
        .getAuthentication();
    String token = "eyJhbGciOiJSUzI1NiJ9.eyJpc3MiOiJFdGVuZG9SWCBBdXRoIiwiaWF0IjoxNzA5NTY4ODIxLCJhZF91c2VyX2lkIjoiMTY3NDUwQTVCQjI4NEFCMjlDNEZFRjAzOUU5OEM5NjMiLCJhZF9jbGllbnRfaWQiOiIyM0M1OTU3NUI5Q0Y0NjdDOTYyMDc2MEVCMjU1QjM4OSIsImFkX29yZ19pZCI6IkU0NDNBMzE5OTJDQjQ2MzVBRkNBRUFCRTcxODNDRTg1IiwiYWRfcm9sZV9pZCI6IkUwM0VENEEwMDU2MzQwNjc4QzNENkEzQkExODNGOTQxIiwic2VhcmNoX2tleSI6ImF1dGgiLCJzZXJ2aWNlX2lkIjoiRkI2NkY5OTQ5QjMzNDgxMEEyRTE1OUMxRDhGMEQ1NDIifQ.mtEcV-jsdjQ70v8iv_CSJ2tvZtP111TofBrUszXhEUwcCYPH43UwLqP67AAYQWY6bvw0C0c16UtTc8cJmsxaDL9TEcpSKAjQ3V8dUls3NUJa1VpSUqv-3zOHa53QSPqi4xjQZeDrPaxab4bSi4UCT49Qblg7rZZNiLT47tsiEfS86uOCxKqJJzty98pVnhHoJdvOOvgoG6BNPq10T7-QWqRatX8aOjWdUty8Ltz-RUwkyXdLGFi4nVpUI55oZapabZcyeNjoiCqp7HX2n0_kzUeh1XGjiVLP2rhHxExi7Tv9BCXPr3pfpxL-VW2dGgAzK616flrvo9zzt1ESb0WOeA";
//    String dasUrl = "http://localhost:8092";
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
