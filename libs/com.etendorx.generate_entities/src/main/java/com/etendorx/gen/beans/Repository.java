/*
 * Copyright 2022  Futit Services SL
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
package com.etendorx.gen.beans;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Repository class needed for code generation
 *
 * @author Sebastian Barrozo
 */
public class Repository {
  private final boolean transactional;
  private final Map<String, RepositorySearch> searches = new HashMap<>();
  private String entityName;

  public Repository(String entityName, boolean transactional) {
    this.entityName = entityName;
    this.transactional = transactional;
  }

  public boolean getTransactional() {
    return transactional;
  }

  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  public Map<String, RepositorySearch> getSearches() {
    return searches;
  }

  public List<HashMap<String, Object>> getSearchesMap() {
    List<HashMap<String, Object>> list = new ArrayList<>();
    for (RepositorySearch repositorySearch : searches.values()) {
      var searchesMap = new HashMap<String, Object>();
      searchesMap.put("query", repositorySearch.getQuery());
      searchesMap.put("method", repositorySearch.getMethod());
      searchesMap.put("params", repositorySearch.getSearchParamsMap());
      searchesMap.put("fetchAttributes", new ArrayList<>());
      list.add(searchesMap);
    }
    return list;
  }
}
