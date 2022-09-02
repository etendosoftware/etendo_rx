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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Projection Entity class needed for code generation
 *
 * @author Sebastian Barrozo
 */
public class ProjectionEntity {
  private static final Logger log = LogManager.getLogger();
  private final String identity;

  private String name;
  private Map<String, ProjectionEntityField> fields = new HashMap<>();
  private String packageName;
  private String className;

  public ProjectionEntity(String name, String identity) {
    this.name = name;
    this.identity = identity;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public Map<String, ProjectionEntityField> getFields() {
    return fields;
  }

  public List<HashMap<String, String>> getFieldsMap() {
      return fields.values().stream().map(v -> {
        var d = new HashMap<String, String>();
        d.put("name", v.getName());
        d.put("value", v.getValue());
        d.put("type", v.getType());
        d.put("className", v.getClassName());
        d.put("projectedEntity", v.getProjectedEntity());
        d.put("projectedField", v.getProjectedField());
        return d;
      }).collect(Collectors.toList());
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public String getPackageName() {
    return packageName;
  }

  public String getIdentity() {
    return identity;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getClassName() {
    return className;
  }

}
