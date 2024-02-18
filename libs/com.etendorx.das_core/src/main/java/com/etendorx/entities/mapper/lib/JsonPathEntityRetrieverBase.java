/*
 * Copyright 2022-2024  Futit Services SL
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
package com.etendorx.entities.mapper.lib;

import org.hibernate.NonUniqueResultException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * This abstract class provides a base implementation for a JsonPathEntityRetriever.
 * It provides methods for retrieving entities from a JPA repository using a set of key values.
 *
 * @param <E> The type of the entity that will be retrieved.
 */
public abstract class JsonPathEntityRetrieverBase<E> implements JsonPathEntityRetriever<E> {

  /**
   * Returns the JPA repository that will be used to retrieve entities.
   *
   * @return The JPA repository.
   */
  public abstract JpaSpecificationExecutor<E> getRepository();

  /**
   * Returns the keys that will be used to retrieve entities.
   *
   * @return The keys.
   */
  public abstract String[] getKeys();

  /**
   * Retrieves an entity from the repository using a single key value.
   *
   * @param key The key value.
   * @return The retrieved entity, or null if no entity was found.
   * @throws NonUniqueResultException If more than one entity was found.
   */
  @Override
  public E get(Object key) throws NonUniqueResultException {
    if (key == null) {
      return null;
    }
    var treeSet = new TreeSet<String>();
    treeSet.add(objectToString(key));
    return get(treeSet);
  }

  /**
   * Retrieves an entity from the repository using a set of key values.
   *
   * @param keyValues The set of key values.
   * @return The retrieved entity, or null if no entity was found.
   * @throws NonUniqueResultException If more than one entity was found.
   */
  @Override
  public E get(TreeSet<String> keyValues) throws NonUniqueResultException {
    Iterator<String> valueIterator = keyValues.iterator();
    if (keyValues.size() != getKeys().length) {
      throw new IllegalArgumentException("Mapping has misconfigured identifiers");
    }
    List<Specification<E>> specs = new ArrayList<>();

    for (String key : getKeys()) {
      String value = valueIterator.next();
      specs.add((root, query, builder) -> builder.equal(root.get(key), value));
    }
    Specification<E> combinedSpec = specs.stream().reduce(Specification::and).orElse(null);
    return getRepository().findOne(combinedSpec).orElse(null);
  }

  /**
   * Converts an object to a string.
   *
   * @param id The object to be converted.
   * @return The string representation of the object.
   * @throws ResponseStatusException If the object cannot be converted to a string.
   */
  private String objectToString(Object id) {
    if (id == null) {
      return null;
    }
    String strId = null;
    if (id instanceof Integer) {
      strId = Integer.toString((Integer) id);
    } else if (id instanceof String) {
      strId = (String) id;
    }
    if (strId == null) {
      throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
          "Invalid value for identifier: " + id + " of type " + id.getClass().getName());
    }
    return strId;
  }
}
