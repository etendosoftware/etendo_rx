package com.etendorx.auth.auth;

import com.etendorx.auth.auth.hashing.PasswordHash;
import com.etendorx.auth.auth.jwt.JwtRequest;
import com.etendorx.auth.feign.ServiceClient;
import com.etendorx.auth.feign.UserClient;
import com.etendorx.auth.feign.model.RxService;
import com.etendorx.auth.feign.model.ServicesAccessModel;
import com.etendorx.auth.feign.model.UserModel;
import com.etendorx.utils.auth.key.JwtKeyUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;

@Component
@Slf4j
public class AuthService {

  public static final String UNAUTHORIZED_MESSAGE = "Invalid username or password.";
  public static final String UNDEFINED_USERNAME_MESSAGE = "The username is not defined.";
  public static final String UNDEFINED_PASSWORD_MESSAGE = "The password is not defined.";
  public static final String UNDEFINED_SERVICE_MESSAGE = "The service is not defined.";
  public static final String UNDEFINED_SECRET_MESSAGE = "The secret is not defined.";
  public static final String UNAUTHORIZED_SECRET_MESSAGE = "The secret is not match.";
  public static final String UNAUTHORIZED_SERVICE_MESSAGE = "The service is not match.";

  public static final String PROJECTION = "auth";
  public static final String HEADER_TOKEN = "X-TOKEN";
  public static final String SUPER_USER_ORG_ID = "0";
  public static final String EMPTY_SERVICE = "";
  public static final String SUPER_USER_ID = "100";

  @Autowired
  private UserClient userClient;

  @Autowired
  private ServiceClient serviceClient;

  @Value("${token}")
  private String token;

  public void validateJwtRequest(JwtRequest jwtRequest) {
    log.debug("Running JWT request validation");
    String username = jwtRequest.getUsername();
    String password = jwtRequest.getPassword();
    String service = jwtRequest.getService();
    String secret = jwtRequest.getSecret();

    if (StringUtils.isEmpty(username)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, UNDEFINED_USERNAME_MESSAGE);
    }
    if (StringUtils.isEmpty(password)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, UNDEFINED_PASSWORD_MESSAGE);
    }
    if (StringUtils.isEmpty(service)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, UNDEFINED_SERVICE_MESSAGE);
    }
    if (StringUtils.isEmpty(secret)) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, UNDEFINED_SECRET_MESSAGE);
    }
  }

  /**
   * Connects with the DAS server to authenticate the user credentials
   *
   * @param username
   *   The username used to log in.
   * @param password
   *   The password used to log in.
   *
   * @return {@link UserModel}
   */
  public UserModel validateCredentials(String username, String password) {
    // Send a request to the DAS server
    log.debug("Sending request to the DAS server.");
    HttpHeaders headers = new HttpHeaders();
    headers.add(HEADER_TOKEN, token);
    ResponseEntity<CollectionModel<UserModel>> modelResponseEntity = userClient.searchUserByUsername(username, "true", PROJECTION, headers);
    if (modelResponseEntity.getStatusCode() != HttpStatus.OK) {
      throw new ResponseStatusException(modelResponseEntity.getStatusCode(), "Unsuccessful DAS connection.");
    }

    CollectionModel<UserModel> collectionModel = modelResponseEntity.getBody();
    if (collectionModel == null || collectionModel.getContent().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
    }

    UserModel userModel = collectionModel.iterator().next();
    // Verify the user password
    if (!PasswordHash.matches(password, userModel.getPassword())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_MESSAGE);
    }

    return userModel;
  }

  public String validateService(UserModel userModel, JwtRequest authRequest) {
    try {
      if (isSuperUser(userModel)) {
        return null;
      }

      List<HashMap<String, String>> accessList = userModel.getETRXRxServicesAccessList();
      if (accessList.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "ServicesAccessList can not be empty. This user do not have access to this service");
      }

      String rxServiceId = accessList.get(0).get("rxServiceId");
      HttpHeaders headers = new HttpHeaders();
      headers.add(HEADER_TOKEN, token);

      ResponseEntity<String> rxServiceResponse = serviceClient.searchServiceByServiceId(rxServiceId, true, headers);
      HttpStatus statusCode = rxServiceResponse.getStatusCode();
      if (statusCode != HttpStatus.OK) {
        throw new ResponseStatusException(statusCode, "Unsuccessful DAS connection.");
      }

      String rxServiceJson = rxServiceResponse.getBody();
      ObjectMapper mapper = new ObjectMapper();
      ServicesAccessModel servicesAccessModel = mapper.readValue(rxServiceJson, ServicesAccessModel.class);
      if (servicesAccessModel == null) {
        throw new ResponseStatusException(HttpStatus.NO_CONTENT, "DAS server respond with not content.");
      }

      List<RxService> rxServicesList = servicesAccessModel.get_embedded().getRxServiceses();
      if (rxServicesList.isEmpty()) {
        throw new ResponseStatusException(HttpStatus.NO_CONTENT, "RxServices list is empty.");
      }

      RxService rxService = rxServicesList.get(0);
      if (!StringUtils.equals(rxService.getSearchkey(), authRequest.getService())) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_SERVICE_MESSAGE);
      }
      if (! StringUtils.equals(rxService.getSecret(), authRequest.getSecret())) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, UNAUTHORIZED_SECRET_MESSAGE);
      }
      return rxService.getSearchkey();
    } catch (RestClientException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during DAS connection.", e);
    } catch (JsonProcessingException e) {
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error during JSON parsing.", e);
    }
  }


  /**
   * Generates JWT claims for a user.
   *
   * @param userModel the user model
   * @return the JWT claims
   */
  public Claims generateUserClaims(UserModel userModel, String searchKey) {
    Claims claims = new DefaultClaims();
    HashMap<String, String> servicesAccess = new HashMap<>();
    if (!isSuperUser(userModel)) {
      servicesAccess = userModel.getETRXRxServicesAccessList().get(0);
    }
    claims.put(JwtKeyUtils.USER_ID_CLAIM, userModel.getId());
    claims.put(JwtKeyUtils.CLIENT_ID_CLAIM, getOrDefaultValue(userModel.getDefaultClientId(), userModel.getClientId()));
    claims.put(JwtKeyUtils.ORG_ID, getOrDefaultValue(servicesAccess.get("defaultOrgId"), SUPER_USER_ORG_ID));
    claims.put(JwtKeyUtils.ROLE_ID, getOrDefaultValue(servicesAccess.get("defaultRoleId"), SUPER_USER_ORG_ID));
    claims.put(JwtKeyUtils.SERVICE_SEARCH_KEY,
            getOrDefaultValue(searchKey, EMPTY_SERVICE));
    claims.put(JwtKeyUtils.SERVICE_ID, getOrDefaultValue(servicesAccess.get("rxServiceId"), EMPTY_SERVICE));
    return claims;
  }

  public static String getOrDefaultValue(String value, String defaultValue) {
    if (value != null && !value.isEmpty()) {
      return value;
    }
    return defaultValue;
  }

  public boolean isSuperUser(UserModel userModel) {
    return StringUtils.equals(userModel.getId(), SUPER_USER_ID);
  }
}
