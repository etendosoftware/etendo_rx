package com.etendorx.das.mapper.lib;

public interface DTOConverter<E, F> {
  F convert(E entity);

  E convert(F entity, E dto);
}
