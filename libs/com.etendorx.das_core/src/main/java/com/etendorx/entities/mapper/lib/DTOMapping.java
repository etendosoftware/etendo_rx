package com.etendorx.das.mapper.lib;

import java.math.BigDecimal;

public interface DTOMapping<T, E> {
  E map(T entity);

}
