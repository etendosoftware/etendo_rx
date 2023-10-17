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
import lombok.val;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

public class BaseDTORepositoryDefault<T extends BaseSerializableObject,E extends BaseDTOModel,F extends BaseDTOModel> implements DASRepository<E,F> {

  private final BaseDASRepository<T> repository;
  private final DTOConverter<T, E, F> converter;
  private final AuditServiceInterceptor auditService;
  private final JsonPathEntityRetriever<T> retriever;

  public BaseDTORepositoryDefault(BaseDASRepository<T> repository,
      DTOConverter<T, E, F> converter,
      JsonPathEntityRetriever<T> retriever,
      AuditServiceInterceptor auditService) {
    this.repository = repository;
    this.converter = converter;
    this.auditService = auditService;
    this.retriever = retriever;
  }

  @Override
  @Transactional
  public Iterable<E> findAll() {
    List<E> dtos = new ArrayList<>();
    Iterable<T> entities = repository.findAll();
    for (val entity : entities) {
      dtos.add(converter.convert(entity));
    }
    return dtos;
  }

  @Transactional
  public E findById(String id) {
    var entity = retriever.get(id);
    return converter.convert(entity);
  }

  @Override
  @Transactional
  public E save(F dtoEntity) {
    if(dtoEntity.getId() != null) {
      var dto = retriever.get(dtoEntity.getId());
      if (dto != null) {
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Record already exists");
      }
    }
    var entity = converter.convert(dtoEntity, null);
    if (BaseRXObject.class.isAssignableFrom(entity.getClass())) {
      var baseObject = (BaseRXObject) entity;
      auditService.setAuditValues(baseObject, true);
    }
    //repository.disableTriggers();
    repository.save(entity);
    //repository.enableTriggers();
    return converter.convert(entity);
  }

  @Override
  @Transactional
  public E updated(F dtoEntity) {
    var entity = retriever.get(dtoEntity.getId());
    if (entity == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
    }
    if(BaseRXObject.class.isAssignableFrom(entity.getClass())) {
      var baseObject = (BaseRXObject) entity;
      auditService.setAuditValues(baseObject, false);
    }
//    repository.disableTriggers();
    repository.save(converter.convert(dtoEntity, entity));
//    repository.enableTriggers();
    return converter.convert(entity);
  }

}
