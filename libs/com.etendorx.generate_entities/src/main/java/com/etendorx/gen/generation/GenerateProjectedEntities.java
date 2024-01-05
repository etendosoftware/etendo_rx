package com.etendorx.gen.generation;

import com.etendorx.gen.beans.Projection;
import com.etendorx.gen.beans.ProjectionEntity;
import com.etendorx.gen.generation.interfaces.ProjectionGenerator;
import com.etendorx.gen.util.TemplateUtil;
import freemarker.template.Template;
import org.apache.commons.lang3.StringUtils;
import org.openbravo.base.model.Entity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Map;

import static com.etendorx.gen.generation.GenerateEntitiesConstants.ENTITY_PACKAGE;
import static com.etendorx.gen.metadata.MetadataUtil.getBasePackageGenLocationPath;

public class GenerateProjectedEntities implements ProjectionGenerator {

  /**
   * Generates the projections and the model projected
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
    if (dataRestEnabled) {
      generateProjections(data, paths.pathEntitiesRx,
          "/org/openbravo/base/gen/datarest/jpaProjectionRX.ftl", projection);
    }
    if (!StringUtils.equals(GenerateEntitiesConstants.PROJECTION_DEFAULT, projection.getName())) {
      generateModelProjected(data, paths.pathEntitiesModelRx, projection,
          (Entity) data.get("entity"));
    }
  }

  /**
   * Generates the projections
   *
   * @param data
   * @param path
   * @param ftlFileName
   * @param projection
   * @throws FileNotFoundException
   */
  private void generateProjections(Map<String, Object> data, String path, String ftlFileName,
      Projection projection) throws FileNotFoundException {
    Template template = TemplateUtil.createTemplateImplementation(ftlFileName);
    String className = data.get("className").toString();
    String projectionName = projection.getName();
    ProjectionEntity projectionEntity = projection.getEntities()
        .getOrDefault(data.get("newClassName").toString(), null);
    String projectionClass = className + StringUtils.capitalize(projectionName) + "Projection.java";
    var outFile = TemplateUtil.prepareOutputFile(path + "/src/main/projections/", projectionClass);
    data.put("projectionName", projectionName);
    data.put("projectionFields",
        projectionEntity != null ? projectionEntity.getFieldsMap() : new ArrayList<String>());
    TemplateUtil.processTemplate(template, data, new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8)));
  }

  /**
   * Generates the java class corresponding with the entity used in a projection.
   * The purpose of the generated object is to store the 'data' fetched using the feign client when making a request.
   *
   * @param data
   * @param pathEntitiesModelRx
   * @param projection
   * @param entity
   */
  private void generateModelProjected(Map<String, Object> data, String pathEntitiesModelRx,
      Projection projection, Entity entity) throws FileNotFoundException {

    String ftlFileNameProjectionRepo = "/org/openbravo/base/gen/entityModelProjected.ftl";
    Template templateModelProjectionRX = TemplateUtil.createTemplateImplementation(
        ftlFileNameProjectionRepo);

    final var projectionName = projection.getName();
    ProjectionEntity projectionEntity = projection.getEntities()
        .getOrDefault(data.get("newClassName").toString(), null);

    final String className = data.get("className").toString();
    final String onlyClassName = data.get("onlyClassName").toString();
    final String packageEntities = data.get("packageEntities").toString();

    String fullPathProjectionRepo = pathEntitiesModelRx + "/src/main/java/" + packageEntities.replace(
        '.', '/');
    final String projectionClass = className.replace(onlyClassName,
        entity.getName()) + StringUtils.capitalize(projectionName) + "Model.java";

    String packageEntityModelProjected = packageEntities + "." + entity.getPackageName();

    if (!StringUtils.equals(GenerateEntitiesConstants.PROJECTION_DEFAULT, projection.getName())) {
      fullPathProjectionRepo = getBasePackageGenLocationPath(
          projection.getModuleLocation()) + File.separator + ENTITY_PACKAGE;
      packageEntityModelProjected = projection.getModuleLocation()
          .getName() + "." + ENTITY_PACKAGE + "." + entity.getPackageName();
    }

    // Set the package of the projected model
    data.put("packageEntityModelProjected", packageEntityModelProjected);

    var outFileProjection = new File(fullPathProjectionRepo, projectionClass);
    new File(outFileProjection.getParent()).mkdirs();

    Writer outWriterProjection = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outFileProjection), StandardCharsets.UTF_8));
    data.put("projectionName", projectionName);
    data.put("projectionFields",
        projectionEntity != null ? projectionEntity.getFieldsMap() : new ArrayList<String>());
    TemplateUtil.processTemplate(templateModelProjectionRX, data, outWriterProjection);
  }

}
