package com.etendorx.gen.generation;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.etendorx.gen.beans.Projection;
import com.etendorx.gen.beans.ProjectionEntity;
import com.etendorx.gen.generation.interfaces.ProjectionGenerator;
import com.etendorx.gen.util.TemplateUtil;

public class GenerateReactCode implements ProjectionGenerator {

  /**
   * Generates the projections and the model projected for a React module
   * @param paths
   * @param data
   * @param projection
   * @throws FileNotFoundException
   */
  @Override
  public void generate(GeneratePaths paths, Map<String, Object> data,
      Projection projection) throws FileNotFoundException {
    generateReactFile(data, projection, "/org/openbravo/base/react/model.types.ftl",
        data.get("newClassName").toString().toLowerCase() + ".types.ts");
    generateReactFile(data, projection, "/org/openbravo/base/react/modelservice.ts.ftl",
        data.get("newClassName").toString().toLowerCase() + "service.ts");
  }

  private void generateReactFile(Map<String, Object> data, Projection projection, String ftlFileName,
      String fileName) throws FileNotFoundException {
    freemarker.template.Template template = TemplateUtil.createTemplateImplementation(ftlFileName);
    var outFile = TemplateUtil.prepareOutputFile(projection.getModuleLocation() + "/lib/data_gen", fileName);
    data.put("projectionName", projection.getName());
    data.put("projectionFields", getProjectionFields(data, projection));
    data.put("projection", projection);
    TemplateUtil.processTemplate(template, data,
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8)));
  }

  private List<Map<String, String>> getProjectionFields(Map<String, Object> data, Projection projection) {
    ProjectionEntity projectionEntity = projection.getEntities().getOrDefault(data.get("newClassName").toString(),
        null);
    return projectionEntity != null ? projectionEntity.getFieldsMap() : new ArrayList<>();
  }

}
