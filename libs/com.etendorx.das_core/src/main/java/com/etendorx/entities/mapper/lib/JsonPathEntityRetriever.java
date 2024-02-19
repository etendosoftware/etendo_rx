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
package com.etendorx.entities.mapper.lib;

import java.util.TreeSet;

/**
 * This interface defines methods for retrieving entities based on JSON paths.
 *
 * @param <E> the type of entity to be retrieved
 */
public interface JsonPathEntityRetriever<E> {

  /**
   * Retrieves an entity based on a single key.
   *
   * @param key the key to use for retrieving the entity
   * @return the retrieved entity
   */
  E get(Object key);

  /**
   * Retrieves an entity based on a field and a key.
   *
   * @param field the field to use for retrieving the entity
   * @param key the key to use for retrieving the entity
   * @return the retrieved entity
   */
  E get(String field, String key);

  /**
   * Retrieves an entity based on a set of key values.
   *
   * @param keyValues the set of key values to use for retrieving the entity
   * @return the retrieved entity
   */
  E get(TreeSet<String> keyValues);
}
