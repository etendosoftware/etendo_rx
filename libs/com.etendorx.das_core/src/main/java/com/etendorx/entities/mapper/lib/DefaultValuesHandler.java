package com.etendorx.entities.mapper.lib;

public interface DefaultValuesHandler {
  void setDefaultValues(Object entity, boolean isNew);

  void triggerEventHandlers(Object entity, boolean isNew);
}
