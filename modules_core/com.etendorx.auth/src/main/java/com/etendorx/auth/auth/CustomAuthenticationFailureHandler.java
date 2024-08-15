package com.etendorx.auth.auth;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {

  @Override
  public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
      AuthenticationException exception) throws IOException, ServletException {
    request.setAttribute("errorMessage", (
        (OAuth2AuthenticationException) exception).getError().getErrorCode()
        .replace("[", "")
        .replace("]", "")
        .replace(" ", "")
    );
    request.setAttribute("errorDescription", (
        (OAuth2AuthenticationException) exception).getError().getDescription());
    request.getRequestDispatcher("/error").forward(request, response);
  }
}

