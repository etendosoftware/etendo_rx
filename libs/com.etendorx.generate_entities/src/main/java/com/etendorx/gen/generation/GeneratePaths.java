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
package com.etendorx.gen.generation;

import java.io.File;

import static com.etendorx.gen.generation.GenerateEntities.MODULES_GEN;

public class GeneratePaths {
  public final String pathEntitiesModelRx;
  public final String pathEtendoRx;
  public final String packageEntities;
  public final String baseRXObject;
  public final String pathEntitiesRx;
  public final String baseDASRepository;
  public final String baseDTORepository;

  GeneratePaths(String pathEtendoRx) {
    this.pathEtendoRx = pathEtendoRx;
    this.pathEntitiesRx = pathEtendoRx + File.separator + MODULES_GEN + File.separator + "com.etendorx.entities";
    this.pathEntitiesModelRx = pathEtendoRx + File.separator + MODULES_GEN + File.separator + "com.etendorx.entitiesModel";
    this.packageEntities = this.pathEntitiesRx.substring(this.pathEntitiesRx.lastIndexOf('/') + 1) + ".entities";
    // Static files from entities
    this.baseRXObject = "BaseRXObject";
    this.baseDASRepository = "BaseDASRepository";
    this.baseDTORepository = "BaseDTORepositoryDefault";
  }
}
