package com.etendorx.auth.config;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import lombok.Getter;

@Component
public class AuthConfig {
  @Getter
  @Value("${token}")
  private String token;

  @Getter
  @Value("${private-key}")
  private String privateKey;

  Logger logger = LoggerFactory.getLogger(getClass());

  @Bean
  public OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService(HttpServletRequest request) {
    return userRequest -> {
      Set<GrantedAuthority> authorities = new LinkedHashSet<>();
      Map<String, Object> userAttributes = new HashMap<>();
      final OAuth2AccessToken accessToken = userRequest.getAccessToken();
      String userNameAttributeName = "name";
      try {
        String userId = (String) request.getSession().getAttribute("userId");
        String etrxOauthProviderId = (String) request.getSession().getAttribute("etrxOauthProviderId");

        fillUserAttributes(userAttributes,
            accessToken.getTokenValue(),
            accessToken.getExpiresAt(),
            accessToken.getIssuedAt(),
            accessToken.getScopes().stream().map(Object::toString).toArray(),
            userId,
            etrxOauthProviderId
        );
      } catch (NullPointerException e) {
        logger.error("Null pointer exception during token attribute generation", e);
        request.setAttribute("errorMessage", "null_attributes");
        throw new AuthenticationServiceException("Failed to generate user attributes due to null token details");
      } catch (Exception e) {
        logger.error("Unexpected error during token attribute generation", e);
        request.setAttribute("errorMessage", "internal_error");
        throw new AuthenticationServiceException("An unexpected error occurred during token attribute generation");
      }
      return new DefaultOAuth2User(authorities, userAttributes, userNameAttributeName);
    };
  }

  private void fillUserAttributes(Map<String, Object> userAttributes, String token,
      Object expiresAt, Object issuedAt, Object scopes, String userId, String etrxOauthProviderId) {
    userAttributes.put("name", "TokenCreatedSuccessfully");
    userAttributes.put("token", token);
    userAttributes.put("expiresAt", expiresAt);
    userAttributes.put("issuedAt", issuedAt);
    userAttributes.put("scopes", scopes);
    userAttributes.put("userId", userId);
    userAttributes.put("etrxOauthProviderId", etrxOauthProviderId);
  }
}
