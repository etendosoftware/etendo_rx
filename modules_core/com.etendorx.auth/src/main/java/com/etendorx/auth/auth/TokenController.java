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
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.etendorx.auth.auth.utils.TokenInfo;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class TokenController {
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

      return generateSuccessHtml();
    } catch (ResourceAccessException e1) {
      request.setAttribute("errorMessage", "conn_refuse_das");
      throw new ResourceAccessException("Connection refused with DAS service. Check if the service is Up and Running");
    }
  }

  public static String generateSuccessHtml() {
    return generateHtml("Execution Successful", "#4caf50", "&#10004;", "#4caf50", "Your request has been processed successfully.");
  }

  public static String generateHtml(String title, String titleColor, String icon, String iconColor, String message) {
    return """
           <!DOCTYPE html>
           <html>
           <head>
               <title>"""
        + title +
        """
        </title>
        <style>
            body {
                font-family: Arial, sans-serif;
                background-color: #f4f4f9;
                margin: 0;
                padding: 0;
                display: flex;
                justify-content: center;
                align-items: center;
                height: 100vh;
            }
            .container {
                background-color: #fff;
                padding: 40px;
                border-radius: 10px;
                box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
                text-align: center;
                max-width: 500px;
                width: 100%;
            }
            h1 {
                color: 
        """
        + titleColor + ";" +
        """
                margin-bottom: 20px;
            }
            p {
                color: #333;
                font-size: 18px;
                margin-bottom: 0;
            }
            .icon {
                font-size: 50px;
                color: 
        """
        + iconColor + ";" +
        """
                margin-bottom: 20px;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="icon">
    """
        + icon +
        """
                </div>
                <h1>
        """
        + title +
        """
                </h1>
                <p>
        """
        + message +
        """
            </p>
            </div>
        </body>
        </html>
        """;
  }
}
