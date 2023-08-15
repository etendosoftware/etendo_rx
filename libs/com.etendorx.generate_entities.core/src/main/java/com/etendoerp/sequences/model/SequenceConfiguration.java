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

package com.etendoerp.sequences.model;

import org.openbravo.base.model.ModelObject;
import org.openbravo.base.model.Reference;

public class SequenceConfiguration extends ModelObject {

  private Reference reference;
  private String generator;
  private String dbSequenceName;
  private String dbSequenceInitial;
  private String dbSequenceIncrement;

  public String getGenerator() {
    return generator;
  }

  public void setGenerator(String generator) {
    this.generator = generator;
  }

  public Reference getReference() {
    return reference;
  }

  public void setReference(Reference reference) {
    this.reference = reference;
  }

  public String getDbSequenceName() {
    return dbSequenceName;
  }

  public void setDbSequenceName(String dbSequenceName) {
    this.dbSequenceName = dbSequenceName;
  }

  public String getDbSequenceInitial() {
    return dbSequenceInitial;
  }

  public void setDbSequenceInitial(String dbSequenceInitial) {
    this.dbSequenceInitial = dbSequenceInitial;
  }

  public String getDbSequenceIncrement() {
    return dbSequenceIncrement;
  }

  public void setDbSequenceIncrement(String dbSequenceIncrement) {
    this.dbSequenceIncrement = dbSequenceIncrement;
  }
}
