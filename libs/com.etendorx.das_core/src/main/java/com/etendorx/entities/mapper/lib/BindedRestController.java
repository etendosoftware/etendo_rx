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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * This class provides a RESTful API for managing entities in the system.
 * It provides basic CRUD operations and uses JsonPathConverter for converting raw JSON strings to DTO objects.
 * It also uses a DASRepository for data access and a Validator for validating DTO objects.
 *
 * @param <E> The type of the DTO model that will be returned in the response.
 * @param <F> The type of the DTO model that will be used for creating and updating entities.
 */
@Log4j2
public abstract class BindedRestController<E extends BaseDTOModel, F extends BaseDTOModel> {

  /**
   * Converter for converting raw JSON strings to DTO objects.
   */
  @Getter
  private final JsonPathConverter<F> converter;

  /**
   * Repository for data access.
   */
  @Getter
  private final DASRepository<E, F> repository;

  /**
   * Validator for validating DTO objects.
   */
  @Getter
  private Validator validator;

  /**
   * Constructor for creating a new instance of BindedRestController.
   *
   * @param converter  Converter for converting raw JSON strings to DTO objects.
   * @param repository Repository for data access.
   * @param validator  Validator for validating DTO objects.
   */
  protected BindedRestController(JsonPathConverter<F> converter, DASRepository<E, F> repository,
      Validator validator) {
    this.converter = converter;
    this.repository = repository;
    this.validator = validator;
  }

  /**
   * Endpoint for getting all entities.
   *
   * @param pageable Pagination information.
   * @return A page of entities.
   */
  @GetMapping
  @Transactional
  @Operation(security = { @SecurityRequirement(name = "basicScheme") })
  public Page<E> findAll(@PageableDefault(size = 20) final Pageable pageable) {
    return repository.findAll(pageable);
  }

  /**
   * Endpoint for getting an entity by its ID.
   *
   * @param id The ID of the entity.
   * @return The entity with the given ID.
   */
  @GetMapping("/{id}")
  @Transactional
  @Operation(security = { @SecurityRequirement(name = "basicScheme") })
  public ResponseEntity<E> get(@PathVariable("id") String id) {
    var entity = repository.findById(id);
    if (entity != null) {
      return new ResponseEntity<>(entity, HttpStatus.OK);
    }
    throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
  }

  /**
   * Endpoint for creating a new entity.
   *
   * @param rawEntity The raw JSON string of the entity to be created.
   * @return The created entity.
   */
  @PostMapping
  @ResponseStatus(HttpStatus.OK)
  @Transactional
  @Operation(security = { @SecurityRequirement(name = "basicScheme") })
  public ResponseEntity<E> post(@RequestBody String rawEntity) {
    try {
      F dtoEntity = converter.convert(rawEntity);
      validate(dtoEntity);
      return new ResponseEntity<>(repository.save(dtoEntity), HttpStatus.CREATED);
    } catch (Exception e) {
      log.error("Error while updating new entity", e);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  /**
   * Endpoint for updating an existing entity.
   *
   * @param id        The ID of the entity to be updated.
   * @param rawEntity The raw JSON string of the entity to be updated.
   * @return The updated entity.
   */
  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @Transactional
  @Operation(security = { @SecurityRequirement(name = "basicScheme") })
  public ResponseEntity<E> put(@PathVariable String id, @RequestBody String rawEntity) {
    if (id == null) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id is required");
    }
    try {
      F dtoEntity = converter.convert(rawEntity);
      validate(dtoEntity);
      dtoEntity.setId(id);
      return new ResponseEntity<>(repository.update(dtoEntity), HttpStatus.CREATED);
    } catch (Exception e) {
      log.error("Error while updating entity {}", id, e);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  /**
   * Validates a DTO object.
   *
   * @param dtoEntity The DTO object to be validated.
   */
  private void validate(F dtoEntity) {
    Set<ConstraintViolation<F>> violations = validator.validate(dtoEntity);
    if (!violations.isEmpty()) {
      List<String> messages = new ArrayList<>();
      for (ConstraintViolation<F> violation : violations) {
        messages.add(violation.getPropertyPath() + ": " + violation.getMessage());
      }
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Validation failed: " + messages);
    }
  }
}
