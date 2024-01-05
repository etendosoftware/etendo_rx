package com.etendorx.gen.generation;

import com.etendorx.gen.generation.interfaces.EntityGenerator;
import com.etendorx.gen.util.TemplateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.model.Entity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GenerateEntityMetadata implements EntityGenerator {
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
  public void generate(Map<String, Object> data, GeneratePaths paths, boolean dataRestEnabled)
      throws FileNotFoundException {
    final String className = ((Entity) data.get("entity")).getName();
    final String fullPathEntities = paths.pathEntitiesRx + GenerateEntitiesConstants.SRC_MAIN_ENTITIES;
    var classfileName = "com/etendorx/entities/metadata/" + className + "_Metadata_" + GenerateEntitiesConstants.JAVA;

    log.debug(GenerateEntities.GENERATING_FILE + classfileName);
    var outFile = new File(fullPathEntities, classfileName);
    new File(outFile.getParent()).mkdirs();
    String ftlFileNameRX = "/org/openbravo/base/gen/entityMetadata.ftl";
    freemarker.template.Template templateRX = TemplateUtil.createTemplateImplementation(
        ftlFileNameRX);
    Writer outWriter = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8));
    TemplateUtil.processTemplate(templateRX, data, outWriter);
  }
}
