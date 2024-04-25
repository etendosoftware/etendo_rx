/*
 * Copyright 2022-2024  Futit Services SL
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
package com.etendorx.entities.mapper.lib;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * This class provides a default implementation of the JsonPathEntityRetriever interface.
 * It extends the JsonPathEntityRetrieverBase class and implements the required methods.
 *
 * @param <E> the type of entity to be retrieved
 */
public class JsonPathEntityRetrieverDefault<E> extends JsonPathEntityRetrieverBase<E> {

  /**
   * The repository used to execute specifications for retrieving entities.
   */
  private final JpaSpecificationExecutor<E> repository;
  private final ExternalIdService externalIdService;
  private final String adTableId;

  /**
   * Constructs a new JsonPathEntityRetrieverDefault object.
   *
   * @param repository the repository to be used for retrieving entities
   */
  public JsonPathEntityRetrieverDefault(JpaSpecificationExecutor<E> repository, ExternalIdService externalIdService, String adTableId) {
    super();
    this.repository = repository;
    this.externalIdService = externalIdService;
    this.adTableId = adTableId;
  }

  @Override
  protected String getTableId() {
    return adTableId;
  }

  @Override
  protected ExternalIdService getExternalIdService() {
    return externalIdService;
  }

  /**
   * Returns the repository used to execute specifications for retrieving entities.
   *
   * @return the repository
   */
  @Override
  public JpaSpecificationExecutor<E> getRepository() {
    return this.repository;
  }

  /**
   * Returns an empty array of keys. This method should be overridden in subclasses
   * if specific keys are required for retrieving entities.
   *
   * @return an empty array of keys
   */
  @Override
  public String[] getKeys() {
    return new String[0];
  }
}
