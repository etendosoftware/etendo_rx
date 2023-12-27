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
package com.etendorx.entities.metadata;

public class FieldMetadata {
  final String type;
  final String dbColumn;
  final String adColumnId;
  final String adTableIdRel;
  final boolean isArray;

  public FieldMetadata(String type, String dbColumn, String adColumnId, String adTableIdRel,
      boolean isArray) {
    this.type = type;
    this.dbColumn = dbColumn;
    this.adColumnId = adColumnId;
    this.adTableIdRel = adTableIdRel;
    this.isArray = isArray;
  }

  public String getType() {
    return type;
  }

  public String getDbColumn() {
    return dbColumn;
  }

  public String getAdColumnId() {
    return adColumnId;
  }

  public String getAdTableIdRel() {
    return adTableIdRel;
  }

  public boolean isArray() {
    return isArray;
  }
}
