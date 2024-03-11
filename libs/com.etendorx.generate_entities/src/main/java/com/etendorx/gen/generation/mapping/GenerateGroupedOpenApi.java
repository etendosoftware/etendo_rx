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
import com.etendorx.gen.generation.GeneratePaths;
import com.etendorx.gen.generation.utils.CodeGenerationUtils;
import com.etendorx.gen.util.TemplateUtil;

import java.io.FileNotFoundException;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenerateGroupedOpenApi {
  private static final String FTL_FILE = "/org/openbravo/base/gen/mappings/groupedOpenApi.ftl"; // NOSONAR
  private static final String OUT_FILE_NAME = "OpenApiConfig.java";

  public void generate(List<ETRXProjection> projections, GeneratePaths path)
      throws FileNotFoundException {
    freemarker.template.Template templateJPARepoRX = TemplateUtil.createTemplateImplementation(
        FTL_FILE);
    final Writer outWriterRepo = CodeGenerationUtils.getInstance().getWriter("", OUT_FILE_NAME, path);
    Map<String, Object> data = new HashMap<>();
    data.put("projections", projections);
    TemplateUtil.processTemplate(templateJPARepoRX, data, outWriterRepo);
  }
}
