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

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Objects;

import org.apache.commons.lang3.reflect.TypeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Utility class for REST calls.
 */
@Component
public class RestUtils {

  private final RestTemplate restTemplate;
  private final String dasUrl;

  @Autowired
  public RestUtils(RestTemplate restTemplate, @Value("${das.url:}") String dasUrl) {
    this.restTemplate = restTemplate;
    this.dasUrl = dasUrl;
  }

  /**
   * Get a list of entities from a URL.
   * The URL must return a HAL list.
   *
   * @param url
   *     URL. Important: Do not include DAS url on the url variable.
   * @param responseType
   *     Entity type
   * @param <T>
   *     Entity type
   * @return List of entities
   * @throws RestUtilsException
   *     If there is an error getting the list
   */
  @Nullable
  public <T> Collection<T> getList(String url, Class<T> responseType) throws RestUtilsException {
    var typeRef = new ParameterizedTypeReference<PagedModel<T>>() {
      @Override
      @NonNull
      public Type getType() {
        return TypeUtils.parameterize(PagedModel.class, responseType);
      }
    };
    ResponseEntity<PagedModel<T>> list = exchange(url, typeRef);
    if (list.getBody() != null) {
      return Objects.requireNonNull(list.getBody()).getContent();
    }
    throw new RestUtilsException("Error getting list: " + list.getStatusCodeValue());
  }

  /**
   * Get an entity from a URL.
   *
   * @param url
   *     URL. Important: Do not include DAS url on the url variable.
   * @param responseType
   *     Entity type
   * @param <T>
   *     Entity type
   * @return Entity
   * @throws RestUtilsException
   *     If there is an error getting the entity
   */
  public <T> T getEntity(String url, Class<T> responseType) throws RestUtilsException {
    var typeRef = new ParameterizedTypeReference<EntityModel<T>>() {
      @Override
      @NonNull
      public Type getType() {
        return TypeUtils.parameterize(EntityModel.class, responseType);
      }
    };
    ResponseEntity<EntityModel<T>> entity = exchange(url, typeRef);
    if(entity.getBody() != null) {
      return Objects.requireNonNull(entity.getBody()).getContent();
    }
    throw new RestUtilsException("Error getting entity: " + entity.getStatusCodeValue());
  }

  /**
   * Perform an exchange.
   *
   * @param url
   *     URL. Important: Do not include DAS url on the url variable.
   * @param responseType
   *     Response type
   * @param <T>
   *     Response type
   * @return Response
   * @throws RestUtilsException
   *     If there is an error performing the exchange
   */
  private <T> ResponseEntity<T> exchange(String url,
      ParameterizedTypeReference<T> responseType) throws RestUtilsException {
    ResponseEntity<T> response = restTemplate.exchange(dasUrl + url, HttpMethod.GET, null, responseType);
    if (response.getStatusCode().is2xxSuccessful()) {
      return response;
    }
    throw new RestUtilsException("Error performing exchange: " + response.getStatusCodeValue());
  }

}
