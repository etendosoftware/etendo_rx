package com.etendorx.gen.generation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.etendorx.gen.generation.interfaces.EntityGenerator;
import com.etendorx.gen.util.TemplateUtil;

public class GenerateEntityRX implements EntityGenerator {
  private static final Logger log = LogManager.getLogger();

  /**
   * Generates the entity RX
   *
   * @param data
   * @param paths
   * @param dataRestEnabled
   * @throws FileNotFoundException
   */
  @Override
  public void generate(Map<String, Object> data, GeneratePaths paths, boolean dataRestEnabled) throws FileNotFoundException {
    final String className = data.get(GenerateEntitiesConstants.CLASS_NAME).toString();
    final String fullPathEntities = paths.pathEntitiesRx + GenerateEntitiesConstants.SRC_MAIN_ENTITIES;
    var classfileName = className + GenerateEntitiesConstants.JAVA;

    log.debug(GenerateEntities.GENERATING_FILE + classfileName);
    var outFile = new File(fullPathEntities, classfileName);
    new File(outFile.getParent()).mkdirs();
    String ftlFileNameRX = "/org/openbravo/base/gen/entityRX.ftl";
    freemarker.template.Template templateRX = TemplateUtil.createTemplateImplementation(
        ftlFileNameRX);
    Writer outWriter = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8));
    TemplateUtil.processTemplate(templateRX, data, outWriter);
  }
}
