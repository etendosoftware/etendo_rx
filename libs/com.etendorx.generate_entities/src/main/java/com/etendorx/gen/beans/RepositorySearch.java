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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * RepositorySearch class needed for code generation
 *
 * @author Sebastian Barrozo
 */
public class RepositorySearch {
  private String query;
  private String method;
  private Map<String, RepositorySearchParam> searchParams = new LinkedHashMap<>();

  public RepositorySearch(String method, String query, Map<String, RepositorySearchParam> searchParams) {
    this.method = method;
    this.query = query;
    this.searchParams = searchParams;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public Map<String, RepositorySearchParam> getSearchParams() {
    return searchParams;
  }


  public List<Map<String, String>> getSearchParamsMap() {
    return searchParams.values().stream().map(v -> {
      var m = new HashMap<String, String>();
      m.put("name", v.getName());
      m.put("type", v.getType());
      return m;
    }).collect(Collectors.toList());
  }
}
