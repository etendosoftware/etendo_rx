package com.etendorx.gen.generation;

import com.etendorx.gen.generation.interfaces.EntityGenerator;
import com.etendorx.gen.util.TemplateUtil;
import freemarker.template.Template;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GenerateEntityModel implements EntityGenerator {

  /**
   * Generates the entity model
   *
   * @param data
   * @param path
   * @param dataRestEnabled
   * @throws FileNotFoundException
   */
  @Override
  public void generate(Map<String, Object> data, GeneratePaths path, boolean dataRestEnabled)
      throws FileNotFoundException {

    String ftlFileNameEntitiesModel = "/org/openbravo/base/gen/entityModel.ftl";
    Template templateEntityModelRX = TemplateUtil.createTemplateImplementation(
        ftlFileNameEntitiesModel);

    final String packageEntityModel = "com.etendorx.entitiesmodel";
    data.put("packageEntityModel", packageEntityModel);
    final String fullPathClientRest = path.pathEtendoRx + "/modules_gen/com.etendorx.entitiesModel" + "/src/main/java/" + packageEntityModel.toLowerCase()
        .replace('.', '/');
    final String repositoryClassEntityModel = data.get("repositoryClassEntityModel").toString();

    var outFileEntityModel = new File(fullPathClientRest, repositoryClassEntityModel);
    new File(outFileEntityModel.getParent()).mkdirs();

    Writer outWriterEntityModel = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outFileEntityModel), StandardCharsets.UTF_8));
    TemplateUtil.processTemplate(templateEntityModelRX, data, outWriterEntityModel);

  }

}
