package com.etendorx.gen.generation;

import static com.etendorx.gen.generation.GenerateEntities.MODULES_GEN;

import java.io.File;

public class GeneratePaths {
  public String pathEntitiesModelRx;
  public String pathEtendoRx;
  public String packageEntities;
  public String baseRXObject;
  public String pathEntitiesRx;

  GeneratePaths(String pathEtendoRx) {
    this.pathEtendoRx = pathEtendoRx;
    this.pathEntitiesRx = pathEtendoRx + File.separator + MODULES_GEN + File.separator + "com.etendorx.entities";
    this.pathEntitiesModelRx = pathEtendoRx + File.separator + MODULES_GEN + File.separator + "com.etendorx.entitiesModel";
    this.packageEntities = this.pathEntitiesRx.substring(this.pathEntitiesRx.lastIndexOf('/') + 1) + ".entities";
    this.baseRXObject = "BaseRXObject";
  }
}
