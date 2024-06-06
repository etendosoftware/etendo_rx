package com.etendorx.auth.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.Collectors;

@Component
public class ParameterExtractionFilter extends OncePerRequestFilter {

  static Logger log = LoggerFactory.getLogger(ParameterExtractionFilter.class);

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws IOException, ServletException {
    Map<String, String> params = parseQueryString(request.getQueryString());
    if (params.containsKey("userId") && params.containsKey("etrxOauthProviderId")) {
      request.getSession().setAttribute("userId", params.get("userId"));
      request.getSession().setAttribute("etrxOauthProviderId", params.get("etrxOauthProviderId"));
    }
    filterChain.doFilter(request, response);
  }

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