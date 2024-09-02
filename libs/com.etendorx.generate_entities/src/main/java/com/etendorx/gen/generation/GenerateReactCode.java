package com.etendorx.gen.generation;

import com.etendorx.gen.beans.Projection;
import com.etendorx.gen.beans.ProjectionEntity;
import com.etendorx.gen.exception.GenerateCodeException;
import com.etendorx.gen.generation.interfaces.ProjectionGenerator;
import com.etendorx.gen.util.TemplateUtil;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generates the projections and the model projected for a React module
 */
public class GenerateReactCode implements ProjectionGenerator {

  private static final String NEW_CLASS_NAME = "newClassName";

  /**
   * Generates the projections and the model projected for a React module
   *
   * @param paths
   * @param data
   * @param projection
   * @param dataRestEnabled
   * @throws FileNotFoundException
   */
  @Override
  public void generate(GeneratePaths paths, Map<String, Object> data, Projection projection,
      boolean dataRestEnabled) throws FileNotFoundException {
    String newClassName = StringUtils.defaultString((String) data.get(NEW_CLASS_NAME));
    generateReactFile(data, projection, "/org/openbravo/base/react/model.types.ftl",
        StringUtils.lowerCase(newClassName) + ".types.ts");
    generateReactFile(data, projection, "/org/openbravo/base/react/modelservice.ts.ftl",
        StringUtils.lowerCase(newClassName) + "service.ts");
    generateReactFile(data, projection, "/org/openbravo/base/react/hookmodel.ts.ftl",
        "use" + newClassName + ".ts");
  }

  private void generateReactFile(Map<String, Object> data, Projection projection,
      String ftlFileName, String fileName) throws FileNotFoundException {
    freemarker.template.Template template = TemplateUtil.createTemplateImplementation(ftlFileName);
    var outFile = TemplateUtil.prepareOutputFile(projection.getModuleLocation() + "/lib/data_gen",
        fileName);
    String newClassName = StringUtils.defaultString((String) data.get(NEW_CLASS_NAME));
    ProjectionEntity projectionEntity = projection.getEntities()
        .getOrDefault(newClassName, null);
    if(projectionEntity == null) {
      throw new GenerateCodeException("Projection entity not found for " + newClassName);
    }

    data.put("projectionName", projection.getName());
    data.put("externalName", projectionEntity.getExternalName());
    data.put("projectionFields", getProjectionFields(projectionEntity));
    data.put("projection", projection);
    TemplateUtil.processTemplate(template, data, new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8)));
  }

  private List<Map<String, String>> getProjectionFields(ProjectionEntity projectionEntity) {
    return projectionEntity != null ? projectionEntity.getFieldsMap() : new ArrayList<>();
  }

}
