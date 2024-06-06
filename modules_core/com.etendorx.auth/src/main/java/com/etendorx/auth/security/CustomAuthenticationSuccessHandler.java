package com.etendorx.auth.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

  @Override
  public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
      Authentication authentication) throws IOException, ServletException {
    DefaultOAuth2User user = (DefaultOAuth2User) authentication.getPrincipal();
    String userId = user.getAttribute("userId");
    String etrxOauthProviderId = user.getAttribute("etrxOauthProviderId");

    if (userId == null || etrxOauthProviderId == null) {
      throw new ServletException("Missing required parameters");
    }

    String redirectUrl = String.format("/api/genToken?userId=%s&etrxOauthProviderId=%s", userId, etrxOauthProviderId);
    response.sendRedirect(redirectUrl);
  }
}
