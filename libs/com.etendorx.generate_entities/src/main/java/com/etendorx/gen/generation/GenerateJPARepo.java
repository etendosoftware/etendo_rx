package com.etendorx.gen.generation;

import com.etendorx.gen.generation.interfaces.EntityGenerator;
import com.etendorx.gen.util.TemplateUtil;
import org.openbravo.base.model.Entity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GenerateJPARepo implements EntityGenerator {

  /**
   * Generates the entity jpa repo
   *
   * @param data
   * @param path
   * @param dataRestEnabled
   * @throws FileNotFoundException
   */
  @Override
  public void generate(Map<String, Object> data, GeneratePaths path, boolean dataRestEnabled)
      throws FileNotFoundException {
    String ftlFileNameJPARepo = "/org/openbravo/base/gen/jpaRepoRX.ftl";
    freemarker.template.Template templateJPARepoRX = TemplateUtil.createTemplateImplementation(
        ftlFileNameJPARepo);

    final String packageJPARepo = path.pathEntitiesRx.substring(
        path.pathEntitiesRx.lastIndexOf('/') + 1) + ".jparepo";
    final String fullPathJPARepo = path.pathEntitiesRx + "/src/main/jparepo/" + packageJPARepo.replace(
        '.', '/');
    final String repositoryClass = ((Entity) data.get("entity")).getName() + "Repository.java";
    new File(fullPathJPARepo).mkdirs();
    var outFileRepo = new File(fullPathJPARepo, repositoryClass);

    data.put("dataRestEnabled", dataRestEnabled);
    data.put("packageJPARepo", packageJPARepo);
    Writer outWriterRepo = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outFileRepo), StandardCharsets.UTF_8));
    TemplateUtil.processTemplate(templateJPARepoRX, data, outWriterRepo);
  }
}
