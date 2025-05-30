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

package com.etendorx.entities.entities;

import com.etendorx.entities.mapper.lib.*;
import com.etendorx.eventhandler.transaction.RestCallTransactionHandler;
import jakarta.transaction.Transactional;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class BaseDTORepositoryDefault<T extends BaseSerializableObject, E extends BaseDTOModel, F extends BaseDTOModel>
    implements DASRepository<E, F> {

  private final RestCallTransactionHandler transactionHandler;
  @Getter
  private final BaseDASRepository<T> repository;
  @Getter
  private final DTOConverter<T, E, F> converter;
  private final AuditServiceInterceptor auditService;
  private final JsonPathEntityRetriever<T> retriever;
  private final Validator validator;
  private final Optional<DefaultValuesHandler> defaultValuesHandler;
  ExternalIdService externalIdService;
  @Value("${"$"}{post-upsert:true}")
  private boolean postUpsert;
  PostSyncService postSyncService;

  public BaseDTORepositoryDefault(RestCallTransactionHandler transactionHandler,
      BaseDASRepository<T> repository, DTOConverter<T, E, F> converter,
      JsonPathEntityRetriever<T> retriever, AuditServiceInterceptor auditService,
      Validator validator, ExternalIdService externalIdService,
      Optional<DefaultValuesHandler> defaultValuesHandler, PostSyncService postSyncService) {
    this.transactionHandler = transactionHandler;
    this.repository = repository;
    this.converter = converter;
    this.auditService = auditService;
    this.retriever = retriever;
    this.validator = validator;
    this.externalIdService = externalIdService;
    this.defaultValuesHandler = defaultValuesHandler;
    this.postSyncService = postSyncService;
  }

  /**
   * Find all entities
   *
   * @return
   */
  @Override
  @Transactional
  public Page<E> findAll(Pageable pageable) {
    Page<T> entities = repository.findAll(pageable);
    return entities.map(converter::convert);
  }

  /**
   * Find entity by id
   *
   * @param id
   * @return
   */
  @Transactional
  public E findById(String id) {
    var entity = retriever.get(id);
    return converter.convert(entity);
  }

  /**
   * Save entity
   *
   * @param dtoEntity
   * @return
   */
  @Override
  public E save(F dtoEntity) {
    return performSaveOrUpdate(dtoEntity, true);
  }

  /**
   * Update entity
   *
   * @param dtoEntity
   * @return
   */
  @Override
  public E update(F dtoEntity) {
    return performSaveOrUpdate(dtoEntity, false);
  }

  /**
   * Perform save or update depending on isNew. This method is transactional and will rollback if
   * any exception is thrown. It will also check for duplicates if the entity has an id.
   *
   * @param dtoEntity
   * @param isNew
   * @return
   */
  private E performSaveOrUpdate(F dtoEntity, boolean isNew) {
    String newId;
    try {
      transactionHandler.begin();
      T existingEntity = null;
      if (!isNew || postUpsert) {
        if (dtoEntity.getId() != null) {
          existingEntity = retriever.get(dtoEntity.getId());
          if (existingEntity != null) {
            isNew = false;
          }
        }
      } else {
        checkForDuplicate(dtoEntity);
      }
      var entity = converter.convert(dtoEntity, existingEntity);
      if (entity == null) {
        throw new IllegalStateException("Entity conversion failed");
      }
      setDefaultValues(entity);
      setAuditValuesIfApplicable(entity);
      Set<ConstraintViolation<T>> violations = validator.validate(entity);
      if (!violations.isEmpty()) {
        List<String> messages = new ArrayList<>();
        boolean hasViolations = false;
        for (ConstraintViolation<T> violation : violations) {
          if (!StringUtils.equals(violation.getPropertyPath().toString(), "id")) {
            messages.add(violation.getPropertyPath() + ": " + violation.getMessage());
            hasViolations = true;
          }
        }
        if (hasViolations) {
          throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
              "Validation failed: " + messages);
        }
      }
      entity = repository.save(entity);
      newId = converter.convert(entity).getId();
      externalIdService.add(entity.getTableId(), dtoEntity.getId(), entity);
      externalIdService.flush();

      entity = converter.convertList(dtoEntity, entity);
      entity = repository.save(entity);
      postSyncService.flush();
      externalIdService.flush();
      transactionHandler.commit();

      triggerEventHandlers(entity, isNew);
      return converter.convert(retriever.get(newId));
    } catch (ResponseStatusException e) {
      throw e;
    } catch (Exception e) {
      e.printStackTrace();
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
    }
  }

  private void triggerEventHandlers(T entity, boolean isNew) {
    if(defaultValuesHandler != null) {
      defaultValuesHandler.ifPresent(
          valuesHandler -> valuesHandler.triggerEventHandlers(entity, isNew));
    }
  }

  private void setDefaultValues(T entity) {
    if(defaultValuesHandler != null) {
      defaultValuesHandler.ifPresent(
          valuesHandler -> valuesHandler.setDefaultValues(entity));
    }
  }

  /**
   * Check for duplicate
   *
   * @param dtoEntity
   */
  public void checkForDuplicate(F dtoEntity) {
    if (dtoEntity.getId() != null && retriever.get(dtoEntity.getId()) != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Record already exists");
    }
  }

  /**
   * Set audit values if applicable
   *
   * @param entity
   */
  private void setAuditValuesIfApplicable(Object entity) {
    if (BaseRXObject.class.isAssignableFrom(entity.getClass())) {
      auditService.setAuditValues((BaseRXObject) entity);
    }
  }

}
