/*
 * Copyright 2022-2024  Futit Services SL
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
package com.etendorx.das.connector;

import com.etendoerp.etendorx.data.ETRXEntityField;
import com.etendoerp.etendorx.data.InstanceConnectorMapping;
import com.etendorx.entities.mapper.lib.DTOReadMapping;
import com.etendorx.entities.metadata.FieldMetadata;
import com.etendorx.entities.metadata.MetadataUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is a component that maps InstanceConnectorMapping entities to a list of maps.
 * It implements the DTOReadMapping interface.
 */
@Component("OBCONFieldMapping")
public class OBCONFieldMapping implements DTOReadMapping<InstanceConnectorMapping> {

  // Utility for metadata operations
  private final MetadataUtil metadataUtil;

  /**
   * Constructor for the OBCONFieldMapping class.
   *
   * @param metadataUtil Utility for metadata operations.
   */
  public OBCONFieldMapping(MetadataUtil metadataUtil) {
    super();
    this.metadataUtil = metadataUtil;
  }

  /**
   * This method maps an InstanceConnectorMapping entity to a list of maps.
   * Each map represents a field in the entity.
   *
   * @param entity The InstanceConnectorMapping entity to map.
   * @return A list of maps representing the fields in the entity.
   */
  @Override
  public Object map(InstanceConnectorMapping entity) {
    List<Map<String, Object>> fieldMapping = new ArrayList<>();
    for (ETRXEntityField etrxEntityField : entity.getEtrxEntityMapping()
        .getProjectionEntity()
        .getETRXEntityFieldList()) {
      Map<String, Object> map = new HashMap<>();
      map.put("name", etrxEntityField.getName());
      map.put("jsonpath", StringUtils.defaultIfBlank(etrxEntityField.getJsonpath(),
          "$." + etrxEntityField.getName()));
      map.put("fieldMapping", etrxEntityField.getFieldMapping());
      map.put("isExternalIdentifier", processProperty(map, etrxEntityField));
      fieldMapping.add(map);
    }
    return fieldMapping;
  }

  /**
   * This method processes a property of an ETRXEntityField.
   * It checks if the property is blank, and if not, it splits the property into segments and retrieves the metadata for the first segment.
   * If the metadata and its related table ID are not null, it adds the table ID to the map and returns true.
   *
   * @param map             The map to add the table ID to.
   * @param etrxEntityField The ETRXEntityField to process the property of.
   * @return True if the property was processed successfully, false otherwise.
   */
  private boolean processProperty(Map<String, Object> map, ETRXEntityField etrxEntityField) {
    String property = etrxEntityField.getProperty();
    if (StringUtils.isBlank(property)) {
      return false;
    }
    String[] propertySegments = property.split("\\.");
    String tableId = etrxEntityField.getEtrxProjectionEntity().getTableEntity().getId();
    String entityName = etrxEntityField.getEtrxProjectionEntity().getTableEntity().getName();
    boolean isProcessProperty = false;

    for (int i = 0; i < propertySegments.length; i++) {
      FieldMetadata field = metadataUtil.getPropertyMetadata(
          tableId,
          propertySegments[i]);
      if (field != null && field.getAdTableIdRel() != null) {
        tableId = field.getAdTableIdRel();
        entityName = field.getEntityName();
        isProcessProperty = true;
      }
    }
    if (isProcessProperty) {
      map.put("ad_table_id", tableId);
      map.put("entityName", entityName);
    }
    return isProcessProperty;
  }
}
