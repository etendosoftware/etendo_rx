package com.etendorx.das.utils;

import com.etendorx.entities.metadata.EntityMetadata;
import com.etendorx.entities.metadata.FieldMetadata;
import com.etendorx.entities.metadata.MetadataUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class MetadataUtilImpl implements MetadataUtil {
  private final Set<EntityMetadata> entityMetadataSet;
  private final Map<String, EntityMetadata> entityByName = new HashMap<>();
  private final Map<String, EntityMetadata> entityByTableId = new HashMap<>();

  public MetadataUtilImpl(Set<EntityMetadata> entityMetadataSet) {
    this.entityMetadataSet = entityMetadataSet;
    entityMetadataSet.forEach(entity -> {
      entityByName.put(entity.getEntityName(), entity);
      entityByTableId.put(entity.getAdTableId(), entity);
    });
  }

  @Override
  public FieldMetadata getPropertyMetadata(String adTableId, String property) {
    var entity = entityByTableId.get(adTableId);
    if(entity != null) {
      return entity.getFields()
          .keySet()
          .stream()
          .filter(key -> StringUtils.equals(key, property))
          .findFirst()
          .map(key -> entity.getFields().get(key))
          .orElse(null);
    }
    return null;
  }
}
