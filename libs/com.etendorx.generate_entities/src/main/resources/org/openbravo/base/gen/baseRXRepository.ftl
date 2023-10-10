package com.etendorx.entities.entities;

import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface BaseRXRepository<E, T>
    extends CrudRepository<E, T>, PagingAndSortingRepository<E, T> {
}
