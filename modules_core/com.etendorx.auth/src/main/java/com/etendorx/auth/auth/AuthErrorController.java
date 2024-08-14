package com.etendorx.auth.auth;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.text.html.HTMLDocument;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.micrometer.common.util.StringUtils;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.log4j.Log4j2;

@Log4j2
@RestController
public class AuthErrorController implements ErrorController {

  @Autowired
  private ResourceLoader resourceLoader;

  @RequestMapping("/error")
  public String handleError(HttpServletRequest request, HttpServletResponse response) {
    String message = (String) request.getAttribute("errorMessage");
    if (log.isDebugEnabled() && StringUtils.isNotBlank(message)) {
      log.debug("Error from errorMessage attribute: {}", message);
    }
    logErrorData(request);
    if (message == null) {
      message = "internal_error";
    }
    String title;
    String errorMessage;
    HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

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

  private static void logErrorData(HttpServletRequest request) {
    Integer statusCode = (Integer) request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
    String errorMessage = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
    String requestUri = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    Throwable exception = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);

    StringBuilder errorDetails = new StringBuilder();
    errorDetails.append("Status Code: ").append(statusCode).append("\n");
    errorDetails.append("Error Message: ").append(errorMessage).append("\n");
    errorDetails.append("Request URI: ").append(requestUri).append("\n");

    if (exception != null) {
      errorDetails.append("Exception: ").append(exception.getClass().getName()).append("\n");
      errorDetails.append("Stack Trace: ").append(Arrays.toString(exception.getStackTrace())).append("\n");
    }
    log.error("Detailed Error: \n{}", errorDetails);
  }

  public String generateHtml(String title, String titleColor, String icon, String iconColor, String message, String loginURL) {
    try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("templates/oAuthResponse.html")) {
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
