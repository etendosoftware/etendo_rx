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
 * Projection class needed for code generation
 *
 * @author Sebastian Barrozo
 */
public class Projection {

  private String name;
  private boolean grpc;
  private boolean react;
  private Map<String, ProjectionEntity> entities = new HashMap<>();

  private File moduleLocation;

  public Projection(String name) {
    this.name = name;
  }

  public Projection(String name, boolean grpc, boolean react) {
    this(name);
    this.grpc = grpc;
    this.react = react;
  }

  public File getModuleLocation() {
    return this.moduleLocation;
  }

  public void setModuleLocation(File moduleLocation) {
    this.moduleLocation = moduleLocation;
  }

  public boolean getGrpc() {
    return this.grpc;
  }

  public boolean getReact() {
    return this.react;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<String, ProjectionEntity> getEntities() {
    return entities;
  }

  public List<HashMap<String, Object>> getEntitiesMap() {
    return entities.values().stream().map(v -> {
      var d = new HashMap<String, Object>();
      d.put("packageName", v.getPackageName());
      d.put("name", v.getName());
      d.put("className", v.getClassName());
      d.put("identity", v.getIdentity());
      d.put("fields", v.getFieldsMap());
      return d;
    }).collect(Collectors.toList());
  }
}
