package com.etendorx.auth.auth;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.common.util.StringUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class AuthErrorController implements ErrorController {

  @Autowired
  private ResourceLoader resourceLoader;

  @RequestMapping("/error")
  public String handleError(HttpServletRequest request, HttpServletResponse response) {
    HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    String message = (String) request.getAttribute("errorMessage");
    if (message == null) {
      message = "internal_error";
    }

    String title;
    String errorMessage;

    switch (message) {
      case "access_denied":
        title = "Access Denied";
        errorMessage = "The login attempt failed due to incorrect credentials or denied access.";
        httpStatus = HttpStatus.FORBIDDEN;
        break;
      case "token_failed":
        title = "Token Creation Failed";
        errorMessage = "Token creation failed! Try again later. If the problem persists, please contact your system administrator.";
        break;
      case "conn_refuse_das":
        title = "Connection Refused";
        errorMessage = "Connection refused with DAS service. Check if the service is Up and Running";
        httpStatus = HttpStatus.SERVICE_UNAVAILABLE;
        break;
      case "null_attributes":
        title = "Null Attributes";
        errorMessage = "Failed to generate user attributes due to null token details";
        httpStatus = HttpStatus.BAD_REQUEST;
        break;
      case "internal_error":
        title = "Internal Error";
        errorMessage = "An internal error occurred.";
        break;
      default:
        title = "Error";
        errorMessage = "An unexpected error occurred: " + httpStatus.getReasonPhrase();
        break;
    }
    String loginURL = (String) request.getSession().getAttribute("loginURL");
    loginURL = StringUtils.isNotBlank(loginURL) ? loginURL : "/login";
    response.setStatus(httpStatus.value());
    return generateHtml(title, "#e74c3c", "&#10006;", "#e74c3c", errorMessage, loginURL);
  }

  public String generateHtml(String title, String titleColor, String icon, String iconColor, String message, String loginURL) {
    try (InputStream inputStream = getClass().getResourceAsStream("/templates/oAuthResponse.html")) {
      if (inputStream == null) {
        throw new RuntimeException("Resource not found: templates/oAuthResponse.html");
      }
      String html = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
      html = html.replace("{{title}}", title)
          .replace("{{titleColor}}", titleColor)
          .replace("{{icon}}", icon)
          .replace("{{iconColor}}", iconColor)
          .replace("{{message}}", message)
          .replace("{{loginURL}}", loginURL);
      return html;
    } catch (IOException e) {
      throw new RuntimeException("Error reading HTML template", e);
    }
  }
}
