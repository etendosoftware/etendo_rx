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

import com.etendoerp.etendorx.model.projection.ETRXProjection;
import com.etendoerp.etendorx.model.projection.ETRXProjectionEntity;
import com.etendorx.gen.generation.GeneratePaths;
import com.etendorx.gen.generation.constants.MappingConstants;
import com.etendorx.gen.generation.utils.CodeGenerationUtils;
import com.etendorx.gen.util.TemplateUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.model.ModelProvider;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generate Base DTO Converter
 */
public class GenerateBaseDTOConverter {

  private static final Logger log = LogManager.getLogger();
  private static final String FTL_FILE = "/org/openbravo/base/gen/mappings/baseDTOConverter.ftl"; // NOSONAR

  /**
   * Generate Base DTO Converter
   *
   * @param projectionEntities
   * @param path
   * @throws FileNotFoundException
   */
  public void generate(List<ETRXProjectionEntity> projectionEntities, GeneratePaths path)
      throws FileNotFoundException {
    freemarker.template.Template templateJPARepoRX = TemplateUtil.createTemplateImplementation(
        FTL_FILE);

    List<String> projectionsId = projectionEntities.stream()
        .map(m -> m.getProjection().getId())
        .distinct()
        .collect(Collectors.toList());
    for (String projectionId : projectionsId) {
      var filteredProjectionEntities = projectionEntities.stream()
          .filter(m -> m.getProjection().getId().equals(projectionId))
          .collect(Collectors.toList());
      ;
      ETRXProjection projection = filteredProjectionEntities.get(0).getProjection();
      // Lookup external names
      List<String> externalNames = filteredProjectionEntities.stream()
          .map(m -> m.getExternalName())
          .distinct()
          .collect(Collectors.toList());
      final String mappingPrefix = projection.getName().toUpperCase();
      for (String externalName : externalNames) {
        ETRXProjectionEntity readEntity = filteredProjectionEntities.stream()
            .filter(e -> StringUtils.equals(e.getMappingType(),
                MappingConstants.MAPPING_TYPE_READ) && StringUtils.equals(e.getExternalName(),
                externalName))
            .findFirst()
            .orElse(null);
        ETRXProjectionEntity writeEntity = filteredProjectionEntities.stream()
            .filter(e -> StringUtils.equals(e.getMappingType(),
                MappingConstants.MAPPING_TYPE_WRITE) && StringUtils.equals(e.getExternalName(),
                externalName))
            .findFirst()
            .orElse(null);
        if (readEntity != null && writeEntity != null && !readEntity.getFields().isEmpty()) {
          TemplateUtil.processTemplate(templateJPARepoRX,
              getData(mappingPrefix, projection, readEntity, writeEntity),
              CodeGenerationUtils.getInstance().getWriter(mappingPrefix, externalName + "DTOConverter.java",
                  path));
        }
      }
    }
  }

  private Map<String, Object> getData(String mappingPrefix, ETRXProjection projection,
      ETRXProjectionEntity readEntity, ETRXProjectionEntity writeEntity) {
    Map<String, Object> data = new HashMap<>();
    data.put("mappingPrefix", mappingPrefix);
    data.put("projection", projection);
    data.put("readEntity", readEntity);
    data.put("writeEntity", writeEntity);
    data.put("modelProvider", ModelProvider.getInstance());
    data.put("genUtils", CodeGenerationUtils.getInstance());
    return data;
  }

}
