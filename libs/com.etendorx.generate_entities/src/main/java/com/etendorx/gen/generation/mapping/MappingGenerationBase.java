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
import com.etendorx.gen.generation.GeneratePaths;
import com.etendorx.gen.generation.interfaces.MappingGenerator;
import com.etendorx.gen.generation.utils.CodeGenerationUtils;
import com.etendorx.gen.util.TemplateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.FileNotFoundException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;

public abstract class MappingGenerationBase implements MappingGenerator {
  private static final Logger log = LogManager.getLogger();
  private final String ftlFileNameRX;

  public MappingGenerationBase(String ftlFileNameRX) {
    this.ftlFileNameRX = ftlFileNameRX;
  }

  /**
   *
   */
  @Override
  public void generate(ETRXProjectionEntity etrxProjectionEntity, GeneratePaths path) throws FileNotFoundException {
    if(!isValid(etrxProjectionEntity)) {
      return;
    }

    if (etrxProjectionEntity.getFields().isEmpty()) {
      log.info("No fields found for entity {}", etrxProjectionEntity.getName());
      return;
    }
    final String outFileName = getOutFileName(etrxProjectionEntity);
    final String mappingPrefix = etrxProjectionEntity.getProjection().getName().toUpperCase();
    final Writer outWriterRepo = CodeGenerationUtils.getWriter(mappingPrefix, outFileName, path);

    freemarker.template.Template templateJPARepoRX = TemplateUtil.createTemplateImplementation(
        ftlFileNameRX);
    TemplateUtil.processTemplate(templateJPARepoRX, getData(mappingPrefix, etrxProjectionEntity), outWriterRepo);
  }

  protected abstract boolean isValid(ETRXProjectionEntity etrxProjectionEntity);

  protected abstract String getOutFileName(ETRXProjectionEntity etrxProjectionEntity);

  private Map<String, Object> getData(String mappingPrefix, ETRXProjectionEntity etrxProjectionEntity) {
    Map<String, Object> data = new HashMap<>();
    data.put("mappingPrefix", mappingPrefix);
    data.put("entity", etrxProjectionEntity);
    return data;
  }

}
