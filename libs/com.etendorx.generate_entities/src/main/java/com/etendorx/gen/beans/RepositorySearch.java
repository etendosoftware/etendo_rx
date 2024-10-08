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

/**
 * RepositorySearch class needed for code generation
 *
 * @author Sebastian Barrozo
 */
public class RepositorySearch {
  private String query;
  private String method;
  private Map<String, RepositorySearchParam> searchParams;

  public RepositorySearch(String method, String query,
      Map<String, RepositorySearchParam> searchParams) {
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
    List<Map<String, String>> list = new ArrayList<>();
    for (RepositorySearchParam repositorySearchParam : searchParams.values()) {
      var m = new HashMap<String, String>();
      m.put("name", repositorySearchParam.getName());
      m.put("type", repositorySearchParam.getType());
      list.add(m);
    }
    return list;
  }
}
