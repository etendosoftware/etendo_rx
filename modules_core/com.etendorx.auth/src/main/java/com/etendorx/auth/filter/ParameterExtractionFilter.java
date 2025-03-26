package com.etendorx.auth.filter;

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
    String user = null;
    String etrxOauthProviderId = null;
    if (params.containsKey("code")) {
      user = (String) request.getSession().getAttribute(USER_ID);
      logger.info("userId from request on ParameterExtractionFilter: " + user);
      etrxOauthProviderId = (String) request.getSession().getAttribute(ETRX_OAUTH_PROVIDER_ID);
      logger.info("etrxOauthProviderId from request on ParameterExtractionFilter: " + etrxOauthProviderId);
    }

    if (params.containsKey("state") && (user == null || etrxOauthProviderId == null)) {
      String encodedState = params.get("state");
      try {
        // Decodificar Base64
        byte[] decodedBytes = Base64.getDecoder().decode(encodedState);
        String decodedState = new String(decodedBytes, StandardCharsets.UTF_8);

        // Convertir el `state` a JSON
        JSONObject jsonState = new JSONObject(decodedState);

        // Obtener los parámetros personalizados enviados en `state`
        String userIdStr = jsonState.optString("userId", null);
        logger.info("userIdStr from state on ParameterExtractionFilter: " + userIdStr);
        String providerIdStr = jsonState.optString("etrxOauthProviderId", null);
        logger.info("providerIdStr from state on ParameterExtractionFilter: " + providerIdStr);
        String originalState = jsonState.optString("state", null);

        // Guardar en sesión para usarlos después
        request.getSession().setAttribute("oauth2_state", originalState);
        request.getSession().setAttribute("userId", userIdStr);
        request.getSession().setAttribute("etrxOauthProviderId", providerIdStr);

      } catch (Exception e) {
        throw new RuntimeException("Error al decodificar el state: " + e.getMessage(), e);
      }
    }

    if ((params.containsKey(USER_ID) && params.containsKey(ETRX_OAUTH_PROVIDER_ID)) ||
        (user != null && etrxOauthProviderId != null)) {
      try {
        logger.info("user from request on ParameterExtractionFilter: " + user
            + "and also (params.containsKey(USER_ID): " + params.containsKey(USER_ID));
        logger.info("etrxOauthProviderId from request on ParameterExtractionFilter: " + etrxOauthProviderId
            + "and also (params.containsKey(ETRX_OAUTH_PROVIDER_ID): " + params.containsKey(ETRX_OAUTH_PROVIDER_ID));
        String fullURL = request.getRequestURL() + "?" + request.getQueryString();
        URI uri = new URI(fullURL);
        request.getSession().setAttribute("loginURL", uri.getPath());
        request.getSession().setAttribute(USER_ID, user != null ? user : params.get(USER_ID));
        request.getSession().setAttribute(ETRX_OAUTH_PROVIDER_ID, etrxOauthProviderId != null ? etrxOauthProviderId : params.get(ETRX_OAUTH_PROVIDER_ID));
      } catch (URISyntaxException e) {
        request.setAttribute("errorMessage" , "internal_error");
        throw new RuntimeException("Error parsing URI", e);
      }
    }
//    if (params.containsKey("code")) {
//      try {
//        String fullURL = request.getRequestURL() + "?" + request.getQueryString();
//        URI uri = new URI(fullURL);
//        request.getSession().setAttribute("code", params.get("code"));
//        request.getSession().setAttribute("loginURL", uri.getPath());
//        request.getSession().setAttribute(USER_ID, "100");
//        request.getSession().setAttribute(ETRX_OAUTH_PROVIDER_ID, "A9739572ADF94BD2AE0963D1637494D6");
//      } catch (URISyntaxException e) {
//        request.setAttribute("errorMessage" , "internal_error");
//        throw new RuntimeException("Error parsing URI", e);
//      }
//    }
      filterChain.doFilter(request, response);
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
