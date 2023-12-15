package com.etendorx.gen.generation;

import static com.etendorx.gen.generation.GenerateEntities.MODULES_GEN;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.openbravo.base.model.Entity;

import com.etendorx.gen.generation.interfaces.EntityGenerator;
import com.etendorx.gen.util.TemplateUtil;

public class GenerateClientRestRX implements EntityGenerator {

  /**
   * Generates the entity
   *
   * @param data
   * @param path
   * @param dataRestEnabled
   * @throws FileNotFoundException
   */
  @Override
  public void generate(Map<String, Object> data, GeneratePaths path, boolean dataRestEnabled) throws FileNotFoundException {
    String pathClientRestRx = path.pathEtendoRx + File.separator + MODULES_GEN + File.separator + "com.etendorx.entitiesModel";
    String ftlFileNameClientRest = "/org/openbravo/base/gen/clientRestRX.ftl";
    freemarker.template.Template templateClientRestRX = TemplateUtil.createTemplateImplementation(
        ftlFileNameClientRest);

    final String packageClientRest = data.get("packageClientRest").toString();
    final String fullPathClientRest = pathClientRestRx + "/src/main/java/" +
        packageClientRest.toLowerCase().replace('.', '/') + "/" +
        ((Entity) data.get("entity")).getPackageName().replace('.', '/');
    final String repositoryClassClientRest = data.get("newClassName") + "ClientRest.java";
    var outFileClientRest = new File(fullPathClientRest, repositoryClassClientRest);
    new File(outFileClientRest.getParent()).mkdirs();

    Writer outWriterClientRest = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outFileClientRest), StandardCharsets.UTF_8));
    data.put("packageClientRest", packageClientRest);
    TemplateUtil.processTemplate(templateClientRestRX, data, outWriterClientRest);

  }
}
