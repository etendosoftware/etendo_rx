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
   * Converts an entity to a DTO (GET Method).
   *
   * @param entity the entity to convert
   * @return the converted DTO
   */
  F convert(E entity);

  /**
   * Converts a DTO to an entity (POST Method).
   *
   * @param dto the DTO to convert
   * @param entity the entity to update
   * @return the updated entity
   */
  E convert(G dto, E entity);

  /**
   * Converts a list of DTOs to a list of entities (POST Method).
   *
   * @param dto the DTO to convert
   * @param entity the entity to update
   * @return the updated entity
   */
  E convertList(G dto, E entity);

  /**
   * Converts a list of entities to a list of DTOs (GET Method).
   *
   * @param entities the list of entities to convert
   * @return a page of converted DTOs
   */
  Page<F> convert(Page<E> entities);

  /**
   * Converts an iterable of entities to an iterable of DTOs (GET Method).
   *
   * @param entities the iterable of entities to convert
   * @return an iterable of converted DTOs
   */
  Iterable<F> convert(Iterable<E> entities);

}
