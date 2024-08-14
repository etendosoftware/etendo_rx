package com.etendorx.auth.auth;

import com.etendorx.auth.auth.jwt.JwtRequest;
import com.etendorx.auth.feign.ServiceClient;
import com.etendorx.auth.feign.model.ServiceAccess;
import com.etendorx.auth.feign.model.UserModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static com.etendorx.auth.auth.AuthService.HEADER_TOKEN;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

public class ServiceValidatorTest {

  @Mock
  private ServiceClient serviceClient;

  private AuthService serviceValidator;

  private UserModel userModel;
  private UserModel regularUser;
  private JwtRequest authRequest;

  @BeforeEach
  public void setup() {
    MockitoAnnotations.initMocks(this);
    serviceValidator = new AuthService();
    userModel.setId("100");
    regularUser.seteTRXRxServicesAccessList(new ArrayList<>());
    authRequest = new JwtRequest("user", "pass", "service", "secret");
  }

  @Test
  public void testValidateService_superUser_returnsNull() {
    String result = serviceValidator.validateService(userModel, authRequest);
    assertNull(result);
  }

  @Test
  public void testValidateService_emptyAccessList_throwsException() {
    assertThrows(ResponseStatusException.class, () -> {
      serviceValidator.validateService(regularUser, authRequest);
    });
  }

  @Test
  public void testValidateService_unsuccessfulDASConnection_throwsException() {
    var serviceAccess = new ServiceAccess();
    serviceAccess.setRxServiceId("123");
    regularUser.seteTRXRxServicesAccessList(List.of(serviceAccess));
    HttpHeaders headers = new HttpHeaders();
    headers.add(HEADER_TOKEN, "token");
    ResponseEntity<String> responseEntity = new ResponseEntity<>("", HttpStatus.BAD_REQUEST);
    when(serviceClient.searchServiceByServiceId("123", true, headers)).thenReturn(responseEntity);

    assertThrows(ResponseStatusException.class, () -> {
      serviceValidator.validateService(regularUser, authRequest);
    });
  }

  @Test
  public void testValidateService_nullServicesAccessModel_throwsException() {
    ServiceAccess serviceAccess = new ServiceAccess();
    serviceAccess.setRxServiceId("123");
    regularUser.seteTRXRxServicesAccessList(List.of(serviceAccess));
    HttpHeaders headers = new HttpHeaders();
    headers.add(HEADER_TOKEN, "token");
    ResponseEntity<String> responseEntity = new ResponseEntity<>("", HttpStatus.OK);
    when(serviceClient.searchServiceByServiceId("123", true, headers)).thenReturn(responseEntity);

    assertThrows(ResponseStatusException.class, () -> {
      serviceValidator.validateService(regularUser, authRequest);
    });
  }
}
