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
package com.etendorx.gen.util;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Metadata class needed for code generation
 *
 * @author Sebastian Barrozo
 */
public class Metadata {
  private final Map<String, Projection> projections = new HashMap<>();
  private final Map<String, Repository> repositories = new HashMap<>();

  // TODO: Should contain the location of the module
  File locationModule;

  public Map<String, Projection> getProjections() {
    return projections;
  }

  public Map<String, Repository> getRepositories() {
    return repositories;
  }

  public List<HashMap<String, Object>> getRepositoriesMap() {
    return repositories.values().stream().map(v -> {
      var d = new HashMap<String, Object>();
      d.put("name", v.getEntityName());
      d.put("transactional", v.getTransactional());
      d.put("searches", v.getSearchesMap());
      return d;
    }).collect(Collectors.toList());
  }
}
