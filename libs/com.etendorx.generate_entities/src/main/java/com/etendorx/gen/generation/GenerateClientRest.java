package com.etendorx.gen.generation;

import static com.etendorx.gen.generation.GenerateEntitiesConstants.CLIENTREST_PACKAGE;
import static com.etendorx.gen.generation.GenerateEntitiesConstants.ENTITY_PACKAGE;
import static com.etendorx.gen.generation.GenerateEntitiesConstants.FTL_FILE_NAME_CLIENTREST;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.openbravo.base.model.Entity;

import com.etendorx.gen.beans.Projection;
import com.etendorx.gen.generation.interfaces.ProjectionGenerator;
import com.etendorx.gen.metadata.MetadataUtil;
import com.etendorx.gen.util.TemplateUtil;

public class GenerateClientRest implements ProjectionGenerator {

  /**
   * Generates the projection
   * @param paths
   * @param data
   * @param projection
   * @throws FileNotFoundException
   */
  @Override
  public void generate(GeneratePaths paths, Map<String, Object> data,
      Projection projection) throws FileNotFoundException {
    freemarker.template.Template templateClientRestRX = TemplateUtil.createTemplateImplementation(
        FTL_FILE_NAME_CLIENTREST);
    var outFileClientRest = TemplateUtil.prepareOutputFile(
        MetadataUtil.getBasePackageGenLocationPath(
            projection.getModuleLocation()) + File.separator + CLIENTREST_PACKAGE,
        data.get("newClassName") + "ClientRest.java");
    data.put("locationModelProjectionClass", getModelProjectionClass(data, projection));
    data.put("feignClientName", getFeignClientName(data, projection));
    data.put("packageClientRestProjected", projection.getModuleLocation().getName() + "." + CLIENTREST_PACKAGE);
    TemplateUtil.processTemplate(templateClientRestRX, data,
        new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFileClientRest), StandardCharsets.UTF_8)));
  }

  /**
   * Obtains the model projection class
   * @param data
   * @param projection
   * @return
   */
  private String getModelProjectionClass(Map<String, Object> data, Projection projection) {
    final String newClassName = data.get("newClassName").toString();
    final String modelProjectionClass = ((Entity) data.get(
        "entity")).getPackageName() + "." + newClassName + StringUtils.capitalize(projection.getName()) + "Model";
    return projection.getModuleLocation().getName() + "." + ENTITY_PACKAGE + "." + modelProjectionClass;
  }

  /**
   * Obtains the feign client name
   * @param data
   * @param projection
   * @return feign client name
   */
  private String getFeignClientName(Map<String, Object> data, Projection projection) {
    String moduleName = projection.getModuleLocation().getName();
    return data.get("newClassName") + "-" + moduleName;
  }
}
