package com.etendorx.auth.filter;

import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AccessDeniedException;
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is a filter that extracts parameters from the query string of the request.
 * It is a Spring component that extends OncePerRequestFilter to ensure it is executed once per request.
 */
@Component
public class ParameterExtractionFilter extends OncePerRequestFilter {

  // Constants for the parameter names to be extracted
  private static final String USER_ID = "userId";
  private static final String ETRX_OAUTH_PROVIDER_ID = "etrxOauthProviderId";
  private static final String ERROR = "error";
  public static final String ERROR_MESSAGE = "errorMessage";
  public static final String STATE = "state";

  /**
   * This method is overridden from OncePerRequestFilter.
   * It extracts parameters from the query string and sets them as attributes in the session.
   * It then allows the request to proceed in the filter chain.
   *
   * @param request the HTTP request
   * @param response the HTTP response
   * @param filterChain the filter chain
   * @throws IOException if an input or output exception occurred
   * @throws ServletException if a servlet exception occurred
   */
  @Override
  protected void doFilterInternal(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain)
      throws IOException, ServletException {

    Map<String, String> params = parseQueryString(request.getQueryString());

    if (params.containsKey(ERROR)) {
      handleError(params.get(ERROR), request);
    }

    if (shouldProcessOAuthParams(params)) {
      processOAuthParams(params, request);
    }

    filterChain.doFilter(request, response);
  }

  /**
   * Handles OAuth errors based on the error parameter received in the request.
   * Sets an appropriate error message and throws specific exceptions for known errors.
   *
   * @param errorValue the value of the 'error' parameter
   * @param request    the current HTTP request
   * @throws AccessDeniedException if the error is "access_denied"
   * @throws RuntimeException      for all other errors
   */
  private void handleError(String errorValue, HttpServletRequest request) throws AccessDeniedException {
    request.setAttribute(ERROR_MESSAGE, errorValue);
    if (StringUtils.equals("access_denied", errorValue)) {
      throw new AccessDeniedException(errorValue);
    }

    request.setAttribute(ERROR_MESSAGE, "internal_error");
    throw new RuntimeException("Internal error occurred.");
  }

  /**
   * Determines whether the request contains parameters that require OAuth processing.
   *
   * @param params the parsed query parameters from the request
   * @return true if OAuth-related processing should occur, false otherwise
   */
  private boolean shouldProcessOAuthParams(Map<String, String> params) {
    return (params.containsKey(USER_ID) && params.containsKey(ETRX_OAUTH_PROVIDER_ID)) ||
        (params.containsKey(STATE) && !params.containsKey("code"));
  }

  /**
   * Extracts user and provider IDs from the request parameters or state token,
   * and stores them in the session if available.
   *
   * @param params  the parsed query parameters
   * @param request the current HTTP request
   */
  private void processOAuthParams(Map<String, String> params, HttpServletRequest request) {
    String userId = params.get(USER_ID);
    String providerId = params.get(ETRX_OAUTH_PROVIDER_ID);

    if (params.containsKey(STATE) && (StringUtils.isBlank(userId) || StringUtils.isBlank(providerId))) {
      JSONObject stateJson = decodeState(params.get(STATE));
      if (StringUtils.isBlank(userId)) {
        userId = stateJson.optString(USER_ID, null);
      }
      if (StringUtils.isBlank(providerId)) {
        providerId = stateJson.optString(ETRX_OAUTH_PROVIDER_ID, null);
      }
    }

    if (userId != null && providerId != null) {
      setSessionAttributes(request, userId, providerId);
    } else {
      logger.warn("userId or etrxOauthProviderId not found in request");
    }
  }

  /**
   * Decodes and parses the 'state' parameter from a URL-safe base64 encoded JSON string.
   *
   * @param encodedState the base64-encoded and URL-decoded state parameter
   * @return a JSONObject representing the decoded state
   * @throws RuntimeException if decoding or parsing fails
   */
  private JSONObject decodeState(String encodedState) {
    try {
      String safeBase64 = URLDecoder.decode(encodedState, StandardCharsets.UTF_8);
      String decoded = new String(Base64.getDecoder().decode(safeBase64));
      return new JSONObject(decoded);
    } catch (Exception e) {
      logger.error("Error decoding or parsing state parameter", e);
      throw new RuntimeException("Invalid state parameter", e);
    }
  }

  /**
   * Stores the user ID and OAuth provider ID in the session, along with the login path.
   *
   * @param request     the current HTTP request
   * @param userId      the user identifier to store
   * @param providerId  the OAuth provider identifier to store
   * @throws RuntimeException if building the URI fails
   */
  private void setSessionAttributes(HttpServletRequest request, String userId, String providerId) {
    try {
      String fullURL = request.getRequestURL() + "?" + request.getQueryString();
      URI uri = new URI(fullURL);

      HttpSession session = request.getSession();
      session.setAttribute("loginURL", uri.getPath());
      session.setAttribute(USER_ID, userId);
      session.setAttribute(ETRX_OAUTH_PROVIDER_ID, providerId);
    } catch (URISyntaxException e) {
      request.setAttribute(ERROR_MESSAGE, "internal_error");
      throw new RuntimeException("Error parsing URI", e);
    }
  }


  /**
   * This method parses the query string into a map of parameter names and values.
   * It splits the query string at '&' to get the parameter pairs, then splits each pair at '=' to get the name and value.
   * If the query string is null, it returns an empty map.
   *
   * @param queryString the query string to parse
   * @return a map of parameter names and values
   */
  private static Map<String, String> parseQueryString(String queryString) {
    if (StringUtils.isBlank(queryString)) return Map.of();
    return Arrays.stream(queryString.split("&"))
        .map(pair -> pair.split("=", 2))
        .collect(Collectors.toMap(
            pair -> pair[0],
            pair -> pair.length > 1 ? pair[1] : ""
        ));
  }
}