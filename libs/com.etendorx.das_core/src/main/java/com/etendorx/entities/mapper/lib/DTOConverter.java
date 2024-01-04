/*
 * Copyright 2022-2023  Futit Services SL
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

import org.springframework.data.domain.Page;

/**
 * Generic interface for DTO converters
 *
 * @param <E> Entity object to convert
 * @param <F> Read DTO model
 * @param <G> Write DTO model
 */
public interface DTOConverter<E, F, G> {

  /**
   * Read Entity and transform to DTO (GET Method)
   *
   * @param entity
   * @return DTO converted
   */
  F convert(E entity);

  /**
   * Read DTO and transform to Entity (POST Method)
   *
   * @param dto
   * @param entity
   * @return Entity updated
   */
  E convert(G dto, E entity);

  /**
   * Read Entity list and transform to DTO list (GET Method)
   *
   * @param entities
   * @return a list of DTOs converted
   */
  Page<F> convert(Page<E> entities);

  /**
   * Read Entity list and transform to DTO list (GET Method)
   *
   * @param entities
   * @return a list of DTOs converted
   */
  Iterable<F> convert(Iterable<E> entities);

  /**
   * @param dto
   * @param entity
   * @return
   */
  E convertOneToMany(G dto, E entity);
}
