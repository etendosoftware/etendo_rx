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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import java.sql.SQLException;

@Log4j2
public abstract class BindedRestController<E extends BaseDTOModel, F extends BaseDTOModel> {
  private final JsonPathConverter<F> converter;
  @Getter
  private final DASRepository<E, F> repository;

  protected BindedRestController(JsonPathConverter<F> converter, DASRepository<E, F> repository) {
    this.converter = converter;
    this.repository = repository;
  }

  @GetMapping
  @Transactional
  @Operation(security = { @SecurityRequirement(name = "basicScheme") })
  public Page<E> findAll(
      @PageableDefault(size = 20) final Pageable pageable) {
    return repository.findAll(pageable);
  }

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

  @PostMapping
  @ResponseStatus(HttpStatus.OK)
  @Transactional
  @Operation(security = { @SecurityRequirement(name = "basicScheme") })
  public ResponseEntity<E> post(@RequestBody String rawEntity) {
    F dtoEntity = converter.convert(rawEntity);
    return new ResponseEntity<>(repository.save(dtoEntity), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  @Transactional
  @Operation(security = { @SecurityRequirement(name = "basicScheme") })
  public ResponseEntity<E> put(@PathVariable String id, @RequestBody String rawEntity) {
    try {
      F dtoEntity = converter.convert(rawEntity);
      dtoEntity.setId(id);
      return new ResponseEntity<>(repository.update(dtoEntity), HttpStatus.CREATED);
    } catch (Exception e) {
      log.error("Error while updating entity {}", id, e);
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }
}
