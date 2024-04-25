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

import lombok.extern.slf4j.Slf4j;
import org.hibernate.Hibernate;
import org.hibernate.NonUniqueResultException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

/**
 * This abstract class provides a base implementation for a JsonPathEntityRetriever.
 * It provides methods for retrieving entities from a JPA repository using a set of key values.
 *
 * @param <E> The type of the entity that will be retrieved.
 */
@Slf4j
public abstract class JsonPathEntityRetrieverBase<E> implements JsonPathEntityRetriever<E> {

  protected abstract String getTableId();

  protected abstract ExternalIdService getExternalIdService();

  /**
   * Returns the JPA repository that will be used to retrieve entities.
   *
   * @return The JPA repository.
   */
  protected abstract JpaSpecificationExecutor<E> getRepository();

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
    return get(getKeys(), keyValues);
  }

/**
 * Retrieves an entity from the repository using a set of key values and their corresponding keys.
 * The keys and values are provided as arrays and must have the same length.
 * Each key-value pair is used to create a specification that checks if an entity's attribute (the key) equals the provided value.
 * All specifications are combined using logical AND, meaning an entity must satisfy all specifications to be retrieved.
 * If no entity satisfies all specifications, null is returned.
 *
 * @param keys An array of keys. Each key is the name of an attribute of the entity.
 * @param keyValues An array of values. Each value corresponds to a key and is the value the entity's attribute should have.
 * @return The retrieved entity, or null if no entity was found.
 * @throws NonUniqueResultException If more than one entity was found.
 * @throws IllegalArgumentException If the number of keys does not match the number of values.
 */
@SuppressWarnings("unchecked")
public E get(String[] keys, TreeSet<String> keyValues) throws NonUniqueResultException {
  Iterator<String> valueIterator = keyValues.iterator();
  if (keyValues.size() != keys.length) {
    throw new IllegalArgumentException("Mapping has misconfigured identifiers");
  }
  List<Specification<E>> specs = new ArrayList<>();

  for (String key : keys) {
    String idReceived = valueIterator.next();
    final String value = getExternalIdService().convertExternalToInternalId(getTableId(), idReceived);
    specs.add((root, query, builder) -> builder.equal(root.get(key), value));
  }
  Specification<E> combinedSpec = specs.stream().reduce(Specification::and).orElse(null);
  if(combinedSpec == null) {
    throw new IllegalArgumentException("No specifications were created");
  }
  var result = getRepository().findAll(combinedSpec);
  if(result.size() > 1) {
    // In case of a bad configuration, the repository will retrieve the first entity that satisfies
    // the specifications. This is a configuration error and should be fixed.
    log.error("Detected a non-unique result for the entity retrieval. This is a configuration error."
        + Arrays.toString(keys));
  }
  // Unproxy the entity to avoid lazy loading issues
  return result.isEmpty() ? null : (E) Hibernate.unproxy(result.get(0));
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

  @Override
  public E get(String field, String key) {
    return get(new String[]{field}, new TreeSet<>(List.of(objectToString(key))));
  }
}
