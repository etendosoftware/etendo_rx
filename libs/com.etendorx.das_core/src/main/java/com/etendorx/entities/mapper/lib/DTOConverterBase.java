package com.etendorx.entities.mapper.lib;

import org.springframework.data.domain.Page;

import java.util.ArrayList;
import java.util.List;

public abstract class DTOConverterBase<E, F, G> implements DTOConverter<E, F, G> {

  @Override
  public Iterable<F> convert(Iterable<E> entities) {
    List<F> dtos = new ArrayList<>();
    for (E entity : entities) {
      dtos.add(convert(entity));
    }
    return dtos;
  }

  @Override
  public Page<F> convert(Page<E> entities) {
    return entities.map(this::convert);
  }

}
