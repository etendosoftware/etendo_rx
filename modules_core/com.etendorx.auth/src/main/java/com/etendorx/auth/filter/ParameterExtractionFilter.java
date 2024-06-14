package com.etendorx.auth.filter;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.util.Map;
import java.util.stream.Stream;
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
    if (params.containsKey(ERROR)) {
      request.setAttribute("errorMessage" , params.get(ERROR));
      if (StringUtils.equals("access_denied", params.get(ERROR))) {
        throw new AccessDeniedException(params.get(ERROR));
      }
      request.setAttribute("errorMessage" , "internal_error");
      throw new RuntimeException("Internal error occurred.");
    }
    if (params.containsKey(USER_ID) && params.containsKey(ETRX_OAUTH_PROVIDER_ID)) {
      request.getSession().setAttribute(USER_ID, params.get(USER_ID));
      request.getSession().setAttribute(ETRX_OAUTH_PROVIDER_ID, params.get(ETRX_OAUTH_PROVIDER_ID));
    }
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
    if (queryString == null) return Map.of();
    return Stream.of(queryString.split("&"))
        .map(pair -> pair.split("="))
        .collect(Collectors.toMap(
            pair -> pair[0],
            pair -> pair.length > 1 ? pair[1] : null
        ));
  }
}