package com.etendorx.gen.generation;

import com.etendorx.gen.beans.Projection;
import com.etendorx.gen.generation.interfaces.ProjectionGenerator;
import com.etendorx.gen.metadata.MetadataUtil;
import com.etendorx.gen.util.TemplateUtil;
import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.model.Entity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static com.etendorx.gen.generation.GenerateEntitiesConstants.*;

public class GenerateClientRest implements ProjectionGenerator {

  /**
   * Generates the projection
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
    freemarker.template.Template templateClientRestRX = TemplateUtil.createTemplateImplementation(
        FTL_FILE_NAME_CLIENTREST);
    var outFileClientRest = TemplateUtil.prepareOutputFile(
        MetadataUtil.getBasePackageGenLocationPath(
            projection.getModuleLocation()) + File.separator + CLIENTREST_PACKAGE,
        data.get("newClassName") + "ClientRest.java");
    data.put("locationModelProjectionClass", getModelProjectionClass(data, projection));
    data.put("feignClientName", getFeignClientName(data, projection));
    data.put("packageClientRestProjected",
        projection.getModuleLocation().getName() + "." + CLIENTREST_PACKAGE);
    TemplateUtil.processTemplate(templateClientRestRX, data, new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outFileClientRest), StandardCharsets.UTF_8)));
  }

  /**
   * Obtains the model projection class
   *
   * @param data
   * @param projection
   * @return
   */
  private String getModelProjectionClass(Map<String, Object> data, Projection projection) {
    final String newClassName = data.get("newClassName").toString();
    final String modelProjectionClass = ((Entity) data.get(
        "entity")).getPackageName() + "." + newClassName + StringUtils.capitalize(
        projection.getName()) + "Model";
    return projection.getModuleLocation()
        .getName() + "." + ENTITY_PACKAGE + "." + modelProjectionClass;
  }

  /**
   * Obtains the feign client name
   *
   * @param data
   * @param projection
   * @return feign client name
   */
  private String getFeignClientName(Map<String, Object> data, Projection projection) {
    String moduleName = projection.getModuleLocation().getName();
    return data.get("newClassName") + "-" + moduleName;
  }
}
