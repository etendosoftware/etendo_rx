package com.etendorx.auth.auth;

import com.etendorx.auth.auth.hashing.PasswordHash;
import com.etendorx.auth.auth.jwt.JwtRequest;
import com.etendorx.auth.feign.UserClient;
import com.etendorx.auth.feign.UserModel;
import com.etendorx.utils.auth.key.JwtKeyUtils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
@Slf4j
public class AuthService {

  public static final String UNAUTHORIZED_MESSAGE = "Invalid username or password.";
  public static final String UNDEFINED_USERNAME_MESSAGE = "The username is not defined.";
  public static final String UNDEFINED_PASSWORD_MESSAGE = "The password is not defined.";

  public static final String PROJECTION = "auth";

  @Autowired
  private UserClient userClient;

  public void validateJwtRequest(JwtRequest jwtRequest) {
    log.debug("Running JWT request validation");
    String username = jwtRequest.getUsername();
    String password = jwtRequest.getPassword();

    if (username == null || username.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, UNDEFINED_USERNAME_MESSAGE);
    }
    if (password == null || password.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, UNDEFINED_PASSWORD_MESSAGE);
    }
  }

  /**
   * Connects with the DAS server to authenticate the user credentials
   *
   * @param username
   *     The username used to log in.
   * @param password
   *     The password used to log in.
   * @return {@link UserModel}
   */
  public UserModel validateCredentials(String username, String password) {
    // Send a request to the DAS server
    log.debug("Sending request to the DAS server.");
    ResponseEntity<CollectionModel<UserModel>> modelResponseEntity = userClient.searchUserByUsername(username, "true",
        PROJECTION);
    if (modelResponseEntity.getStatusCode() != HttpStatus.OK) {
      throw new ResponseStatusException(modelResponseEntity.getStatusCode(), "Unsuccessful DAS connection.");
    }

    CollectionModel<UserModel> collectionModel = modelResponseEntity.getBody();
    if (collectionModel == null) {
      throw new ResponseStatusException(HttpStatus.NO_CONTENT, "DAS server respond with not content.");
    }

    // There is not a 'AdUser' created with the provided username
    if (collectionModel.getContent().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
    }

    UserModel userModel = collectionModel.iterator().next();
    // Verify the user password
    if (!PasswordHash.matches(password, userModel.getPassword())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
    }

    return userModel;
  }

  public Claims generateUserClaims(UserModel userModel) {
    Claims claims = new DefaultClaims();
    claims.put(JwtKeyUtils.USER_ID_CLAIM, userModel.getId());
    claims.put(JwtKeyUtils.CLIENT_ID_CLAIM, getOrDefaultValue(userModel.getDefaultClientId(), userModel.getClientId()));
    claims.put(JwtKeyUtils.ORG_ID_CLAIM,
        getOrDefaultValue(userModel.getDefaultOrganizationId(), userModel.getOrganizationId()));
    return claims;
  }

  public static String getOrDefaultValue(String value, String defaultValue) {
    if (value != null && !value.isEmpty()) {
      return value;
    }
    return defaultValue;
  }

}
