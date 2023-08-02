package com.etendorx.das.mapper.lib;


public interface DASRepository<E extends BaseDTOModel> {

  Iterable<E> findAll();

  E findById(String id);

  E save(E dtoEntity);

  E updated(E dtoEntity);
}
