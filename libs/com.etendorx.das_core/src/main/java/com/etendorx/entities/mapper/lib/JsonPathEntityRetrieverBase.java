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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public abstract class JsonPathEntityRetrieverBase<E> implements JsonPathEntityRetriever<E> {

  public abstract JpaSpecificationExecutor<E> getRepository();

  public abstract String[] getKeys();

  @Override
  public E get(String key) {
    var treeSet = new TreeSet<String>();
    treeSet.add(key);
    return get(treeSet);
  }

  @Override
  public E get(TreeSet<String> keyValues) {
    Iterator<String> valueIterator = keyValues.iterator();
    if (keyValues.size() != getKeys().length) {
      throw new IllegalArgumentException("Invalid number of key values");
    }
    List<Specification<E>> specs = new ArrayList<>();

    for (String key : getKeys()) {
      String value = valueIterator.next();
      specs.add((root, query, builder) -> builder.equal(root.get(key), value));
    }

    Specification<E> combinedSpec = specs.stream().reduce(Specification::and).orElse(null);
    return getRepository().findOne(combinedSpec).orElse(null);
  }
}