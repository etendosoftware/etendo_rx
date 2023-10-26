package com.etendorx.edge.filters.auth;

import com.etendorx.utils.auth.key.JwtKeyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.GatewayFilterFactory;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.security.PublicKey;


@Component
public class JwtAuthenticationFilter implements GatewayFilterFactory<JwtAuthenticationFilter.Config> {

  public static final String TOKEN_HEADER = "X-TOKEN";
  public static final String PUBLIC_KEY_LOCATION = "public.key.location";
  public static final String PUBLIC_KEY_ENV = "public.key.env";

  @Autowired
  Environment env;

  @Value("${public-key}")
  private String publicKeyValue;

  @PostConstruct
  public void init() throws IOException {
    loadPublicKey();
  }

  private PublicKey publicKey;

  private void loadPublicKey() throws IOException {
    this.publicKey = JwtKeyUtils.readPublicKey(publicKeyValue);
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (ServerWebExchange exchange, GatewayFilterChain chain) -> {
      ServerHttpRequest request = (ServerHttpRequest) exchange.getRequest();
      HttpHeaders headers = request.getHeaders();

      if (!headers.containsKey(TOKEN_HEADER)) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.BAD_REQUEST);
        return response.setComplete();
      }

      String token = headers.getOrEmpty(TOKEN_HEADER).get(0);

      if (!JwtKeyUtils.isValidToken(this.publicKey, token)) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        return response.setComplete();
      }

      return chain.filter(exchange);
    };
  }

  @Override
  public Class<Config> getConfigClass() {
    return Config.class;
  }

  @Override
  public Config newConfig() {
    return new Config("JwtAuthenticationFilter");
  }

  public static class Config {
    public Config(String name) {
      this.name = name;
    }

    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }
}
