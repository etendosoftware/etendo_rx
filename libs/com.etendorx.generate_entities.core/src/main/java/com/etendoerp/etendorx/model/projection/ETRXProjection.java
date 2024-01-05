package com.etendoerp.etendorx.model.projection;

import com.etendoerp.etendorx.model.ETRXModule;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.model.ModelObject;

import java.util.Set;

@JsonIncludeProperties({ "name", "grpc", "entities" })
public class ETRXProjection extends ModelObject {

  private static final Logger log = LogManager.getLogger();

  private ETRXModule module;
  private String name;
  private boolean grpc;
  private Set<ETRXProjectionEntity> entities;

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void setName(String name) {
    this.name = name;
  }

  public boolean isGrpc() {
    return grpc;
  }

  public void setGrpc(boolean grpc) {
    this.grpc = grpc;
  }

  public ETRXModule getModule() {
    return module;
  }

  public void setModule(ETRXModule module) {
    this.module = module;
  }

  public Set<ETRXProjectionEntity> getEntities() {
    return entities;
  }

  public void setEntities(Set<ETRXProjectionEntity> entities) {
    this.entities = entities;
  }
}
