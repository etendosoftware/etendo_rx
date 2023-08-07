/*
 * Copyright 2023  Futit Services SL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.etendorx.clientrest.base;

import static com.etendorx.clientrest.base.ClientRestConstants.DAS_AUTH_METHOD_GLOBAL;
import static com.etendorx.clientrest.base.ClientRestConstants.X_TOKEN;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Factory for RestTemplate with HAL support.
 */
@Component
public class TokenRequestInterceptor implements ClientHttpRequestInterceptor {
  private final String token;
  private final String method;

  public TokenRequestInterceptor(@Value("${token:}") String token,
      @Value("${das.auth.method:global}") String method) {
    this.token = token;
    this.method = method;
  }

  /**
   * This method intercepts each outgoing HTTP request and modifies its headers before
   * sending. Specifically, it adds the "Accept" header and optionally the "X-TOKEN" header
   * based on configuration.
   */
  @Override
  public ClientHttpResponse intercept(HttpRequest request, byte[] body,
      ClientHttpRequestExecution execution) throws IOException {
    request.getHeaders().add("Accept", "*/*");

    if (DAS_AUTH_METHOD_GLOBAL.equals(method) && StringUtils.hasText(token)) {
      request.getHeaders().set(X_TOKEN, token);
    }

    return execution.execute(request, body);
  }
}
