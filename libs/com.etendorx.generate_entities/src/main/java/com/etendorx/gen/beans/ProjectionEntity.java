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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Projection Entity class needed for code generation
 *
 * @author Sebastian Barrozo
 */
public class ProjectionEntity {
  private static final Logger log = LogManager.getLogger();
  private final Boolean identity;

  private String name;
  private Map<String, ProjectionEntityField> fields = new HashMap<>();
  private String packageName;
  private String className;

  public ProjectionEntity(String name, Boolean identity) {
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

  public String getPackageName() {
    return packageName;
  }

  public void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  public Boolean getIdentity() {
    return identity;
  }

  public String getClassName() {
    return className;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public List<Map<String, String>> getFieldsMap() {
    return fields.values().stream().map(v -> {
      var fieldsMap = new HashMap<String, String>();
      fieldsMap.put("name", v.getName());
      fieldsMap.put("value", v.getValue());
      fieldsMap.put("type", v.getType());
      fieldsMap.put("className", v.getClassName());
      fieldsMap.put("projectedEntity", v.getProjectedEntity());
      fieldsMap.put("projectedField", v.getProjectedField());
      fieldsMap.put("notNullValue", v.getNotNullValue());
      return fieldsMap;
    }).collect(Collectors.toList());
  }

}
