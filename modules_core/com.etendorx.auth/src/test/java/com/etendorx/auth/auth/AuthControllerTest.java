package com.etendorx.auth.auth;

import com.etendorx.auth.auth.jwt.JwtRequest;
import com.etendorx.auth.auth.jwt.JwtResponse;
import com.etendorx.auth.auth.jwt.JwtService;
import com.etendorx.auth.feign.model.UserModel;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

public class AuthControllerTest {

  @Mock
  private AuthService authServices;

  @Mock
  private JwtService jwtService;

  @Mock
  private UserModel userModel;

  @InjectMocks
  private AuthController authController;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  public void testAuthentication() throws Exception {
    // Arrange
    JwtRequest request = new JwtRequest();
    request.setUsername("username");
    request.setPassword("password");

    userModel.setId(String.valueOf(1L));
    userModel.setUsername("username");
    userModel.setPassword("password");

    String searchKey = "searchKey";

    Claims claims = Jwts.claims();
    claims.setSubject("subject");
    claims.put("userId", userModel.getId());

    KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("EC");
    ECGenParameterSpec ecSpec = new ECGenParameterSpec("secp256r1");
    keyPairGenerator.initialize(ecSpec);
    KeyPair keyPair = keyPairGenerator.generateKeyPair();
    ECPrivateKey ecPrivateKey = (ECPrivateKey) keyPair.getPrivate();

    String token = Jwts.builder()
        .setClaims(claims)
        .signWith(SignatureAlgorithm.ES256, ecPrivateKey)
        .compact();

    doNothing().when(authServices).validateJwtRequest(request);
    when(authServices.validateCredentials(request.getUsername(), request.getPassword())).thenReturn(
        userModel);
    when(authServices.validateService(userModel, request)).thenReturn(searchKey);
    when(authServices.generateUserClaims(userModel, searchKey)).thenReturn(claims);
    when(jwtService.generateJwtToken(claims)).thenReturn(new JwtResponse(token));

    // Act
    ResponseEntity<JwtResponse> response = authController.authentication(request);

    // Assert
    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertNotNull(response.getBody());
    assertEquals(token, response.getBody().getToken());

    // Verify
    verify(authServices).validateJwtRequest(request);
  }

}

