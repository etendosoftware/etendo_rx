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
package com.etendoerp.etendorx.model.projection;

import java.math.BigDecimal;

import com.etendoerp.etendorx.model.mapping.ETRXJavaMapping;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.openbravo.base.model.ModelObject;

@JsonIncludeProperties({"name", "value"})
public class ETRXEntityField extends ModelObject {

  private ETRXProjectionEntity entity;
  private String property;
  private Boolean isMandatory;
  private Boolean identifiesUnivocally;
  private String fieldMapping;
  private BigDecimal line;
  private ETRXJavaMapping javaMapping;

  private ETRXProjectionEntity etrxProjectionEntityRelated;
  private String jsonPath;

  public ETRXProjectionEntity getEntity() {
    return entity;
  }

  public void setEntity(ETRXProjectionEntity entity) {
    this.entity = entity;
  }

  @JsonProperty("value")
  public String getProperty() {
    return property;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public Boolean getIsMandatory() {
    return isMandatory;
  }

  public void setIsMandatory(Boolean mandatory) {
    isMandatory = mandatory;
  }

  public Boolean getIdentifiesUnivocally() {
    return identifiesUnivocally;
  }

  public void setIdentifiesUnivocally(Boolean identifiesUnivocally) {
    this.identifiesUnivocally = identifiesUnivocally;
  }

  public String getFieldMapping() {
    return fieldMapping;
  }

  public ETRXJavaMapping getJavaMapping() {
    return javaMapping;
  }

  public void setJavaMapping(ETRXJavaMapping javaMapping) {
    this.javaMapping = javaMapping;
  }

  public void setFieldMapping(String fieldMapping) {
    this.fieldMapping = fieldMapping;
  }

  public Boolean getMandatory() {
    return isMandatory;
  }

  public void setMandatory(Boolean mandatory) {
    isMandatory = mandatory;
  }

  public ETRXProjectionEntity getEtrxProjectionEntityRelated() {
    return etrxProjectionEntityRelated;
  }

  public void setEtrxProjectionEntityRelated(
      ETRXProjectionEntity etrxProjectionEntityRelated) {
    this.etrxProjectionEntityRelated = etrxProjectionEntityRelated;
  }

  public BigDecimal getLine() {
    return line;
  }

  public void setLine(BigDecimal line) {
    this.line = line;
  }

  public String getJsonPath() {
    return jsonPath;
  }

  public void setJsonPath(String jsonPath) {
    this.jsonPath = jsonPath;
  }
}
