package com.etendorx.auth.auth;

import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

import com.etendorx.auth.auth.jwt.JwtRequest;
import com.etendorx.auth.auth.jwt.JwtResponse;
import com.etendorx.auth.feign.model.UserModel;

import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import net.minidev.json.parser.ParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
public class AuthController {

  @Autowired
  AuthService authServices;

  @Autowired
  private Environment environment;

  @PostMapping("/authenticate")
  @PreAuthorize("hasAuthority('SCOPE_GUEST')")
  public ResponseEntity<JwtResponse> authentication(@RequestBody JwtRequest authRequest) {
    authServices.validateJwtRequest(authRequest);
    UserModel userModel = authServices.validateCredentials(authRequest.getUsername(),
        authRequest.getPassword());
    String searchKey = authServices.validateService(userModel, authRequest);
    JSONObject claims = authServices.generateUserClaims(userModel, searchKey);
    String classicUrl = environment.getProperty("classic.url");
    return sendSWSTokenRequest(classicUrl + "/sws/login", claims);
  }

  private ResponseEntity<JwtResponse> sendSWSTokenRequest(String url, JSONObject jsonBody) {
    HttpClient httpClient = HttpClient.newHttpClient();

    HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .header("accept", "application/json")
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(jsonBody.toString()))
        .build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() == 200) {
        String jsonBodyStr = response.body();
        JSONParser parser = new JSONParser(JSONParser.MODE_JSON_SIMPLE);
        JSONObject responseBody = (JSONObject) parser.parse(jsonBodyStr);

        String token = (String) responseBody.get("token");
        JwtResponse jwtResponse = new JwtResponse(token);

        return ResponseEntity.ok(jwtResponse);
      } else {
        return ResponseEntity.status(HttpStatus.valueOf(response.statusCode())).build();
      }
    } catch (InterruptedException | IOException | ParseException e) {
      log.error("Error sending request to SWS", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  public static Properties loadGradleProperties() {
    Properties properties = new Properties();
    try (FileReader reader = new FileReader("gradle.properties")) {
      properties.load(reader);
    } catch (Exception e) {
      log.error("Error loading gradle.properties", e);
    }
    return properties;
  }
}
