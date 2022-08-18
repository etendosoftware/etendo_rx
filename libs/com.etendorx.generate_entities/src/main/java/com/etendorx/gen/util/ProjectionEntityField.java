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

public class ProjectionEntityField {
  private String name;
  private String value;
  private String type;
  private String className;
  private String projectedEntity;
  private String projectedField;

  public ProjectionEntityField(String name, String value, String type) {
    this.name = name;
    this.value = value;
    this.type = type;
  }

  public ProjectionEntityField(String name, String value, String type, String className) {
    this.name = name;
    this.value = value;
    this.type = type;
    this.className = className;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public void setClassName(String className) {
    this.className = className;
  }

  public String getClassName() {
    return this.className;
  }

  public String getProjectedEntity() {
    return projectedEntity;
  }

  public void setProjectedEntity(String projectedEntity) {
    this.projectedEntity = projectedEntity;
  }

  public String getProjectedField() {
    return projectedField;
  }

  public void setProjectedField(String projectedField) {
    this.projectedField = projectedField;
  }
}
