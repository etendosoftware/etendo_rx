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
package com.etendorx.gen.generation.mapping;

import com.etendoerp.etendorx.model.projection.ETRXProjectionEntity;
import com.etendorx.gen.generation.constants.MappingConstants;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

public class GenerateBaseRestController extends MappingGenerationBase {
  private static final String FTL_FILE = "/org/openbravo/base/gen/mappings/baseRestController.ftl"; // NOSONAR

  public GenerateBaseRestController() {
    super(FTL_FILE);
  }

  @Override
  protected boolean isValid(ETRXProjectionEntity etrxProjectionEntity) {
    return (
        StringUtils.equals(etrxProjectionEntity.getMappingType(), MappingConstants.MAPPING_TYPE_READ) && BooleanUtils.isTrue(etrxProjectionEntity.isRestEndPoint())
    );
  }

  @Override
  protected String getOutFileName(ETRXProjectionEntity etrxProjectionEntity) {
    return etrxProjectionEntity.getTable().getName() + "RestController.java";
  }

}
