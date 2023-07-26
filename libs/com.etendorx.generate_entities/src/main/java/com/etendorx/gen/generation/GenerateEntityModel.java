package com.etendorx.gen.generation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.etendorx.gen.generation.interfaces.EntityGenerator;
import com.etendorx.gen.util.TemplateUtil;

import freemarker.template.Template;

public class GenerateEntityModel implements EntityGenerator {

  /**
   * Generates the entity model
   * @param data
   * @param path
   * @throws FileNotFoundException
   */
  @Override
  public void generate(Map<String, Object> data, GeneratePaths path) throws FileNotFoundException {

    String ftlFileNameEntitiesModel = "/org/openbravo/base/gen/entityModel.ftl";
    Template templateEntityModelRX = TemplateUtil.createTemplateImplementation(
        ftlFileNameEntitiesModel);

    final String packageEntityModel = "com.etendorx.entitiesmodel";
    data.put("packageEntityModel", packageEntityModel);
    final String fullPathClientRest = path.pathEtendoRx + "/modules_gen/com.etendorx.entitiesModel" + "/src/main/java/" + packageEntityModel.toLowerCase().replace(
        '.', '/');
    final String repositoryClassEntityModel = data.get("repositoryClassEntityModel").toString();

    var outFileEntityModel = new File(fullPathClientRest, repositoryClassEntityModel);
    new File(outFileEntityModel.getParent()).mkdirs();

    Writer outWriterEntityModel = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outFileEntityModel), StandardCharsets.UTF_8));
    TemplateUtil.processTemplate(templateEntityModelRX, data, outWriterEntityModel);

  }

}
