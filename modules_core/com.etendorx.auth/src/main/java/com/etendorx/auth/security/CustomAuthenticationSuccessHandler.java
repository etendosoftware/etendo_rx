package com.etendorx.auth.security;

import com.nimbusds.oauth2.sdk.util.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * This class is a custom implementation of the AuthenticationSuccessHandler interface.
 * It is used to handle successful authentication events.
 * It is a Spring component.
 */
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  // The URI for generating a token with parameters
  private static final String API_GEN_TOKEN_URI_WITH_PARAMS = "/api/genToken?userId=%s&etrxOauthProviderId=%s";
  public static final String USER_ID = "userId";
  public static final String ETRX_OAUTH_PROVIDER_ID = "etrxOauthProviderId";

  /**
   * This method is called when an authentication attempt is successful.
   * It retrieves the authenticated user's attributes and redirects the response to the token generation URI.
   *
   * @param request the HTTP request
   * @param response the HTTP response
   * @param authentication the authentication object which was created during the authentication process
   * @throws IOException if an input or output exception occurred
   * @throws ServletException if a servlet exception occurred
   */
  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {
    DefaultOAuth2User user = (DefaultOAuth2User) authentication.getPrincipal();
    String userId = StringUtils.isNotBlank((String) user.getAttribute(USER_ID))
        ? (String) user.getAttribute(USER_ID)
        : String.valueOf(request.getSession().getAttribute(USER_ID));
    String etrxOauthProviderId = StringUtils.isNotBlank((String) user.getAttribute(ETRX_OAUTH_PROVIDER_ID))
        ? (String) user.getAttribute(ETRX_OAUTH_PROVIDER_ID)
        : String.valueOf(request.getSession().getAttribute(ETRX_OAUTH_PROVIDER_ID));


    // Check if the required parameters are present
    if (userId == null || etrxOauthProviderId == null) {
      throw new ServletException("Missing required parameters");
    }

    // Format the redirect URL with the user's attributes
    String redirectUrl = String.format(API_GEN_TOKEN_URI_WITH_PARAMS, userId, etrxOauthProviderId);
    // Redirect the response to the token generation URI
    response.sendRedirect(redirectUrl);
  }
}