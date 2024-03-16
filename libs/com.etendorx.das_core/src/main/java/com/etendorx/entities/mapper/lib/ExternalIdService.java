package com.etendorx.entities.mapper.lib;

public interface ExternalIdService {
  void getExternalId(String entityName, String entityId, String externalId) ;

  void add(String adTableId, String externalId, Object entity);

  void flush();
}
