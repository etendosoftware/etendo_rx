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

package org.openbravo.base.model;

import com.etendoerp.sequences.model.SequenceConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.etendorx.base.exception.OBException;
import org.etendorx.base.util.OBClassLoader;
import org.openbravo.base.model.domaintype.DomainType;
import org.openbravo.base.model.domaintype.PrimitiveDomainType;
import org.openbravo.base.model.domaintype.StringDomainType;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.Set;

/**
 * Used by the {@link ModelProvider ModelProvider}, maps the AD_Reference table in the in-memory
 * model.
 *
 * @author iperdomo
 */
public class Reference extends ModelObject {
  private static final Logger log = LogManager.getLogger();

  // Ids of ReferenceTypes
  public static final String TABLE = "18";
  public static final String TABLEDIR = "19";
  public static final String SEARCH = "30";
  public static final String IMAGE = "32";
  public static final String IMAGE_BLOB = "4AA6C3BE9D3B4D84A3B80489505A23E5";
  public static final String RESOURCE_ASSIGNMENT = "33";
  public static final String PRODUCT_ATTRIBUTE = "35";
  public static final String NO_REFERENCE = "-1";

  private static HashMap<String, Class<?>> primitiveTypes;

  static {
    // Mapping reference id with a Java type
    primitiveTypes = new HashMap<String, Class<?>>();

    primitiveTypes.put("10", String.class);
    primitiveTypes.put("11", Long.class);
    primitiveTypes.put("12", BigDecimal.class);
    primitiveTypes.put("13", String.class);
    primitiveTypes.put("14", String.class);
    primitiveTypes.put("15", Date.class);
    primitiveTypes.put("16", Date.class);
    primitiveTypes.put("17", String.class);
    primitiveTypes.put("20", Boolean.class);
    primitiveTypes.put("22", BigDecimal.class);
    primitiveTypes.put("23", byte[].class); // Binary/Blob Data
    primitiveTypes.put("24", Timestamp.class);
    primitiveTypes.put("26", Object.class); // RowID is not used
    primitiveTypes.put("27", Object.class); // Color is not used
    primitiveTypes.put("28", Boolean.class);
    primitiveTypes.put("29", BigDecimal.class);
    primitiveTypes.put("34", String.class);
    primitiveTypes.put("800008", BigDecimal.class);
    primitiveTypes.put("800019", BigDecimal.class);
    primitiveTypes.put("800101", String.class);
  }

  private String modelImpl;
  private DomainType domainType;
  private Reference parentReference;
  private boolean baseReference;
  private Set<SequenceConfiguration> sequenceConfigurations;

  public boolean isBaseReference() {
    return baseReference;
  }

  public void setBaseReference(boolean baseReference) {
    this.baseReference = baseReference;
  }

  public boolean isPrimitive() {
    return getDomainType() instanceof PrimitiveDomainType;
  }

  public DomainType getDomainType() {
    if (domainType != null) {
      return domainType;
    }
    String modelImplementationClass = getModelImplementationClassName();
    if (modelImplementationClass == null) {
      log.error(
          "Reference " + this + " has a modelImpl which is null, using String as the default");
      modelImplementationClass = StringDomainType.class.getName();
    }
    try {
      final Class<?> clz = OBClassLoader.getInstance().loadClass(modelImplementationClass);
      domainType = (DomainType) clz.getDeclaredConstructor().newInstance();
      domainType.setReference(this);
    } catch (Exception e) {
      throw new OBException(
          "Not able to create domain type " + getModelImpl() + " for reference " + this, e);
    }
    return domainType;
  }

  /**
   * Also calls the parent reference ({@link #getParentReference()}) to find the modelImpl (
   * {@link #getModelImpl()}).
   *
   * @return the modelImpl or if not set, the value set in the parent.
   */
  public String getModelImplementationClassName() {
    // only call the parent if the parent is a base reference and this is not a basereference
    if (getModelImpl() == null && !isBaseReference() && getParentReference() != null && getParentReference().isBaseReference()) {
      return getParentReference().getModelImplementationClassName();
    }
    return getModelImpl();
  }

  public String getModelImpl() {
    return modelImpl;
  }

  public void setModelImpl(String modelImpl) {
    this.modelImpl = modelImpl;
  }

  public Reference getParentReference() {
    return parentReference;
  }

  public void setParentReference(Reference parentReference) {
    this.parentReference = parentReference;
  }

  public void setSequenceConfigurations(Set<SequenceConfiguration> sequenceConfigurations) {
    this.sequenceConfigurations = sequenceConfigurations;
  }

  public Set<SequenceConfiguration> getSequenceConfigurations() {
    return sequenceConfigurations;
  }

}
