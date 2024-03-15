package com.etendorx.auth.auth;

import com.etendorx.auth.auth.jwt.JwtRequest;
import com.etendorx.auth.auth.jwt.JwtResponse;
import com.etendorx.auth.auth.jwt.JwtService;
import com.etendorx.auth.feign.model.UserModel;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AuthController {

  @Autowired
  JwtService jwtService;

  @Autowired
  AuthService authServices;

  @PostMapping("/authenticate")
  @PreAuthorize("hasAuthority('SCOPE_GUEST')")
  public ResponseEntity<JwtResponse> authentication(@RequestBody JwtRequest authRequest) {
    authServices.validateJwtRequest(authRequest);
    UserModel userModel = authServices.validateCredentials(authRequest.getUsername(),
        authRequest.getPassword());
    String searchKey = authServices.validateService(userModel, authRequest);
    Claims claims = authServices.generateUserClaims(userModel, searchKey);
    return new ResponseEntity<>(jwtService.generateJwtToken(claims), HttpStatus.OK);
  }
}
