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

import com.etendorx.entities.mapper.lib.BaseDTOModel;
import com.etendorx.entities.mapper.lib.DASRepository;
import com.etendorx.entities.mapper.lib.DTOConverter;
import com.etendorx.entities.mapper.lib.JsonPathEntityRetriever;
import com.etendorx.eventhandler.transaction.RestCallTransactionHandler;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.val;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

public class BaseDTORepositoryDefault<T extends BaseSerializableObject,E extends BaseDTOModel,F extends BaseDTOModel> implements DASRepository<E,F> {

  private final RestCallTransactionHandler transactionHandler;
  @Getter
  private final BaseDASRepository<T> repository;
  @Getter
  private final DTOConverter<T, E, F> converter;
  private final AuditServiceInterceptor auditService;
  private final JsonPathEntityRetriever<T> retriever;

  public BaseDTORepositoryDefault(RestCallTransactionHandler transactionHandler,
      BaseDASRepository<T> repository, DTOConverter<T, E, F> converter,
      JsonPathEntityRetriever<T> retriever, AuditServiceInterceptor auditService) {
    this.transactionHandler = transactionHandler;
    this.repository = repository;
    this.converter = converter;
    this.auditService = auditService;
    this.retriever = retriever;
  }

  /**
   * Find all entities
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
   * @param dtoEntity
   * @return
   */
  @Override
  public E save(F dtoEntity) {
    return performSaveOrUpdate(dtoEntity, true);
  }

  /**
   * Update entity
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
   * @param dtoEntity
   * @param isNew
   * @return
   */
  private E performSaveOrUpdate(F dtoEntity, boolean isNew) {
    String newId;
    try {
      transactionHandler.begin();
      if (isNew) {
        checkForDuplicate(dtoEntity);
      }
      var entity = converter.convert(dtoEntity, isNew ? null : retriever.get(dtoEntity.getId()));
      if (entity == null) {
        throw new IllegalStateException("Entity conversion failed");
      }
      setAuditValuesIfApplicable(entity, isNew);
      repository.save(entity);
      repository.save(converter.convertOneToMany(dtoEntity, entity));
      newId = converter.convert(entity).getId();
    } finally {
      transactionHandler.commit();
    }
    return converter.convert(retriever.get(newId));
  }

  /**
   * Check for duplicate
   * @param dtoEntity
   */
  private void checkForDuplicate(F dtoEntity) {
    if (dtoEntity.getId() != null && retriever.get(dtoEntity.getId()) != null) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Record already exists");
    }
  }

  /**
   * Set audit values if applicable
   * @param entity
   * @param isNew
   */
  private void setAuditValuesIfApplicable(Object entity, boolean isNew) {
    if (BaseRXObject.class.isAssignableFrom(entity.getClass())) {
      auditService.setAuditValues((BaseRXObject) entity, isNew);
    }
  }

}
