package com.etendorx.das.mapper.lib;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ResponseStatusException;

public abstract class BindedRestController<E extends BaseDTOModel> {
  private final DASRepository<E> repository;

  public BindedRestController(
      DASRepository<E> repository) {
    this.repository = repository;
  }

  @GetMapping
  public ResponseEntity<Iterable<E>> findAll() {
    return new ResponseEntity<>(repository.findAll(), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<E> get(@PathVariable("id") String id) {
    var entity = repository.findById(id);
    if (entity != null) {
      return new ResponseEntity<>(entity, HttpStatus.OK);
    } else {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
    }
  }

  @PostMapping
  public ResponseEntity<E> post(E dtoEntity) {
    return new ResponseEntity<>(repository.save(dtoEntity), HttpStatus.CREATED);
  }

  @PutMapping("/{id}")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<E> put(@PathVariable String id, @RequestBody E dtoEntity) {
    dtoEntity.setId(id);
    return new ResponseEntity<>(repository.updated(dtoEntity), HttpStatus.CREATED);
  }
}
