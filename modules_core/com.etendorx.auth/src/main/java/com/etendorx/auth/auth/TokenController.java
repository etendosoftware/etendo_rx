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

@RestController()
public class TokenController {
  private static final String CENTERED_DIV = "<div style=\"display: flex;align-items: center; justify-content: center; text-align: center;\">";
  private static final String CLOSED_DIV = "</div>";
  private static final String AUTH_TOKEN_INFO_URI_WITH_DATE_FORMAT = "/auth/ETRX_Token_Info?_dateFormat=yyyy-MM-dd'T'HH:mm:ss.SSSX";
  @Value("${token}")
  String token;
  @Value("${das.url}")
  String dasUrl;

  @GetMapping("/api/genToken")
  public String index(@RequestHeader Map<String, String> headers, @RequestParam(required = false) String userId,
      @RequestParam(required = false) String etrxOauthProviderId) {
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
    new RestTemplate().exchange(dasUrl + AUTH_TOKEN_INFO_URI_WITH_DATE_FORMAT,
        HttpMethod.POST,
        new HttpEntity<>(tokenInfo, tokenHeaders),
        TokenInfo.class);
    authentication.setAuthenticated(false);
    return generateSuccessHtml();
//    return CENTERED_DIV + "Token Created!" + CLOSED_DIV +
//        CENTERED_DIV + "You can close this window." + CLOSED_DIV;
  }

  /**
   * Genera un HTML que indica una ejecución exitosa.
   *
   * @return Un string que contiene el HTML.
   */
  public static String generateSuccessHtml() {
    // Plantilla HTML para una ejecución exitosa
    return "<!DOCTYPE html>\n"
        + "<html>\n"
        + "<head>\n"
        + "    <title>Execution Successful</title>\n"
        + "    <style>\n"
        + "        body { \n"
        + "            font-family: Arial, sans-serif; \n"
        + "            background-color: #f4f4f9; \n"
        + "            margin: 0; \n"
        + "            padding: 0; \n"
        + "            display: flex; \n"
        + "            justify-content: center; \n"
        + "            align-items: center; \n"
        + "            height: 100vh; \n"
        + "        }\n"
        + "        .container { \n"
        + "            background-color: #fff; \n"
        + "            padding: 40px; \n"
        + "            border-radius: 10px; \n"
        + "            box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1); \n"
        + "            text-align: center; \n"
        + "            max-width: 500px; \n"
        + "            width: 100%; \n"
        + "        }\n"
        + "        h1 { \n"
        + "            color: #4caf50; \n"
        + "            margin-bottom: 20px; \n"
        + "        }\n"
        + "        p { \n"
        + "            color: #333; \n"
        + "            font-size: 18px; \n"
        + "            margin-bottom: 0; \n"
        + "        }\n"
        + "        .success-icon { \n"
        + "            font-size: 50px; \n"
        + "            color: #4caf50; \n"
        + "            margin-bottom: 20px; \n"
        + "        }\n"
        + "    </style>\n"
        + "</head>\n"
        + "<body>\n"
        + "    <div class=\"container\">\n"
        + "        <div class=\"success-icon\">&#10004;</div>\n"
        + "        <h1>Execution Successful</h1>\n"
        + "        <p>Your request has been processed successfully.</p>\n"
        + "    </div>\n"
        + "</body>\n"
        + "</html>";
  }
}
