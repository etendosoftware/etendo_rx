package com.etendorx.das.mapper.lib;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.etendorx.entities.entities.BaseDASRepository;
import com.etendorx.entities.entities.BaseRXObject;

import lombok.val;

public class BaseDTORepositoryDefault<T extends BaseRXObject, E extends BaseDTOModel> implements DASRepository<E> {

  private final BaseDASRepository<T> repository;
  private final DTOConverter<T, E> converter;

  public BaseDTORepositoryDefault(BaseDASRepository<T> repository,
      DTOConverter<T, E> converter) {
    this.repository = repository;
    this.converter = converter;
  }

  @Override
  public Iterable<E> findAll() {
    List<E> dtos = new ArrayList<>();
    Iterable<T> entities = repository.findAll();
    for (val entity : entities) {
      dtos.add(converter.convert(entity));
    }
    return dtos;
  }

  public E findById(String id) {
    var entity = repository.findById(id);
    return entity.map(converter::convert).orElse(null);
  }

  @Override
  @Transactional
  public E save(E dtoEntity) {
    var entity = converter.convert(dtoEntity, null);
    repository.disableTriggers();
    repository.save(entity);
    return converter.convert(entity);
  }

  @Override
  @Transactional
  public E updated(E dtoEntity) {
    var entity = repository.findById(dtoEntity.getId());
    if (entity.isEmpty()) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
    }
    repository.disableTriggers();
    repository.save(converter.convert(dtoEntity, entity.get()));
    repository.enableTriggers();
    return converter.convert(entity.get());
  }

}
