package com.etendorx.auth.auth;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class AuthErrorController implements ErrorController {

  @Autowired
  private ResourceLoader resourceLoader;

  @RequestMapping("/error")
  public String handleError(HttpServletRequest request, HttpServletResponse response, @RequestParam(required = false) String errorMessage) throws IOException {
    HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    String message = request.getAttribute("errorMessage") != null ?
        (String) request.getAttribute("errorMessage") : "internal_error";

    String title = "";
    message = switch (message) {
      case "access_denied" -> {
        title = "Access Denied";
        yield "The login attempt failed due to incorrect credentials or denied access.";
      }
      case "token_failed" -> {
        title = "Token Creation Failed";
        yield "Token creation failed! Try again later. If the problem persists, please contact your system administrator.";
      }
      case "conn_refuse_das" -> {
        title = "Connection Refused";
        yield "Connection refused with DAS service. Check if the service is Up and Running";
      }
      case "null_attributes" -> {
        title = "Null Attributes";
        yield "Failed to generate user attributes due to null token details";
      }
      case "internal_error" -> {
        title = "Internal Error";
        yield "An internal error occurred.";
      }
      default -> {
        // Default case for unexpected status codes
        title = "Error";
        yield "An unexpected error occurred: " + httpStatus.getReasonPhrase();
      }
    };

    return generateHtml(title, "#e74c3c", "&#10006;", "#e74c3c", message);
  }

  public static String generateHtml(String title, String titleColor, String icon, String iconColor, String message) {
    return """
           <!DOCTYPE html>
           <html>
           <head>
               <title>"""
        + title +
        """
        </title>
        <style>
            body {
                font-family: Arial, sans-serif;
                background-color: #f4f4f9;
                margin: 0;
                padding: 0;
                display: flex;
                justify-content: center;
                align-items: center;
                height: 100vh;
            }
            .container {
                background-color: #fff;
                padding: 40px;
                border-radius: 10px;
                box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
                text-align: center;
                max-width: 500px;
                width: 100%;
            }
            h1 {
                color: 
        """
        + titleColor + ";" +
        """
                margin-bottom: 20px;
            }
            p {
                color: #333;
                font-size: 18px;
                margin-bottom: 0;
            }
            .icon {
                font-size: 50px;
                color: 
        """
        + iconColor + ";" +
        """
                margin-bottom: 20px;
            }
        </style>
    </head>
    <body>
        <div class="container">
            <div class="icon">
    """
        + icon +
        """
                </div>
                <h1>
        """
        + title +
        """
                </h1>
                <p>
        """
        + message +
        """
            </p>
            </div>
        </body>
        </html>
        """;
  }
}
