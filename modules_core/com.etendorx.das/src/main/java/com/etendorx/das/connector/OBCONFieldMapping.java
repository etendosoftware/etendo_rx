package com.etendorx.das.connector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.etendoerp.etendorx.data.ETRXEntityField;
import com.etendoerp.etendorx.data.InstanceConnectorMapping;
import com.etendorx.entities.jparepo.ADTableRepository;
import com.etendorx.entities.mapper.lib.DTOReadMapping;
import com.etendorx.entities.metadata.MetadataUtil;


@Component("OBCONFieldMapping")
public class OBCONFieldMapping implements DTOReadMapping<InstanceConnectorMapping> {

  @Autowired
  ADTableRepository adTableRepository;

  @Autowired
  private MetadataUtil metadataUtil;


  @Override
  public Object map(InstanceConnectorMapping entity) {
    List<Map<String, String>> fieldMapping = new ArrayList<>();
    for (ETRXEntityField etrxEntityField : entity.getEtrxEntityMapping().getProjectionEntity().getETRXEntityFieldList()) {
      Map<String, String> map = new HashMap<>();
      map.put("name", etrxEntityField.getName());
      map.put("jsonpath", etrxEntityField.getJsonpath());
      map.put("fieldMapping", etrxEntityField.getFieldMapping());
      var field = metadataUtil.getPropertyMetadata(
          entity.getEtrxEntityMapping().getProjectionEntity().getTableEntity().getId(), etrxEntityField.getProperty());
      map.put("ad_table_id", field != null ? field.getAdTableIdRel() : null);
      fieldMapping.add(map);
    }
    return fieldMapping;
  }
}