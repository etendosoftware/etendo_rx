/*
 * Copyright 2022  Futit Services SL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.etendorx.gen.process;

import com.etendorx.gen.beans.Metadata;
import com.etendorx.gen.beans.Projection;
import com.etendorx.gen.beans.ProjectionEntity;
import com.etendorx.gen.metadata.MetadataContainer;
import com.etendorx.gen.metadata.MetadataUtil;
import com.etendorx.gen.util.CodeGenerationException;
import com.etendorx.gen.util.TemplateUtil;
import freemarker.template.Template;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.model.Entity;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.etendorx.gen.metadata.MetadataProjectionsAnalyzer.getProjectionEntity;
import static com.etendorx.gen.metadata.MetadataProjectionsAnalyzer.getProjectionEntityField;

public class GenerateProtoFile {
  private static final Logger log = LogManager.getLogger();

  private List<Entity> entitiesModel = new ArrayList<>();
  private Map<String, Entity> entitiesModelMap = new HashMap<>();

  /**
   * Generates a unique projection from a module metadata.
   * The projections contain a merge from multiple projections with the same entity.
   * The entity will contain all the declared fields.
   *
   * @param moduleMetadata   The module from where the projection will be obtained
   * @param projectionFilter A lambda function used to filter the module projections
   * @return A Projection.
   */
  public static Projection generateProjectionMix(Metadata moduleMetadata,
      Predicate<Projection> projectionFilter) {
    Projection projectionMix = new Projection("mix");

    moduleMetadata.getProjections()
        .values()
        .stream()
        .filter(projectionFilter)
        .flatMap(p -> p.getEntities().values().stream())
        .forEach(entity -> {
          ProjectionEntity projectionEntityMix = getProjectionEntity(projectionMix,
              entity.getName(), entity.getName(), false);
          entity.getFields()
              .values()
              .forEach(field -> getProjectionEntityField(projectionEntityMix, field.getName(),
                  field.getValue(), field.getType()));
        });

    return projectionMix;
  }

  /**
   * Generates the proto files for the given projections.
   *
   * @param pathEtendoRx
   * @param repositories
   * @param projections
   * @param metadataContainer
   * @param computedColumns
   * @param includeViews
   * @throws FileNotFoundException
   */
  public void generate(String pathEtendoRx, List<HashMap<String, Object>> repositories,
      Collection<Projection> projections, MetadataContainer metadataContainer,
      boolean computedColumns, boolean includeViews) throws FileNotFoundException {

    this.generate(pathEtendoRx, metadataContainer, computedColumns, includeViews);

    var filteredProjections = projections.stream()
        .filter(
            projection -> projection.getName().compareTo("default") != 0 && projection.getGrpc())
        .collect(Collectors.toList());

    for (Projection projection : filteredProjections) {

      generateGrpcService(pathEtendoRx, projection, repositories, computedColumns, includeViews);

      generateGRPCDto(pathEtendoRx, projection, repositories, computedColumns, includeViews);

      generateGRPCDtoProjection(pathEtendoRx, projection, repositories, computedColumns,
          includeViews);

      generateProjectionDTO2Grpc(pathEtendoRx, projection, repositories, computedColumns,
          includeViews);

      generateClientServiceInterface(pathEtendoRx, projection, repositories, computedColumns,
          includeViews);

      generateClientGrpcService(pathEtendoRx, projection, repositories, computedColumns,
          includeViews);
    }
  }

  /**
   * Generates the proto files for the given projections.
   *
   * @param pathEtendoRx
   * @param metadataContainer
   * @param computedColumns
   * @param includeViews
   * @throws FileNotFoundException
   */
  private void generate(String pathEtendoRx, MetadataContainer metadataContainer,
      boolean computedColumns, boolean includeViews) throws FileNotFoundException {

    // Filter the metadata modules which contains a projection with the 'grpc' set to true.
    List<Metadata> grpcModulesMetadata = metadataContainer.getMetadataList()
        .stream()
        .filter(
            metadata -> metadata.getProjections().values().stream().anyMatch(Projection::getGrpc))
        .collect(Collectors.toList());

    for (Metadata moduleMetadata : grpcModulesMetadata) {
      generateProtoFile(pathEtendoRx, moduleMetadata, computedColumns, includeViews);
    }
  }

  /**
   * Generates the proto file for the given module metadata.
   *
   * @param pathEtendoRx
   * @param moduleMetadata
   * @param computedColumns
   * @param includeViews
   * @throws FileNotFoundException
   */
  private void generateProtoFile(String pathEtendoRx, Metadata moduleMetadata,
      boolean computedColumns, boolean includeViews) throws FileNotFoundException {

    var outFileDir = pathEtendoRx + "/modules_gen/com.etendorx.grpc.common/src/main/proto";
    new File(outFileDir).mkdirs();

    String modulePackageName = moduleMetadata.getLocationModule().getName();
    log.info("* Generating proto file for: {}", modulePackageName);
    var outFile = new File(outFileDir, modulePackageName + ".proto");

    String ftlFile = "/org/openbravo/base/process/proto.ftl";
    Template template = TemplateUtil.createTemplateImplementation(ftlFile);

    // Projection mix containing the same entity with multiple 'fields' obtained from other 'grpc' projections entities.
    Projection projectionMix = generateProjectionMix(moduleMetadata, Projection::getGrpc);

    // If the 'repositories' uses an entity not registered in the projections. Add the default one with all the fields.
    moduleMetadata.getRepositories().forEach((entityName, repository) -> {
      if (!projectionMix.getEntities().containsKey(entityName)) {
        Entity entity = this.entitiesModelMap.get(entityName);
        if (entity == null) {
          throw new RuntimeException(
              "Error generating proto file for '" + modulePackageName + "'. The entity '" + entityName + "' is not defined.");
        }
        ProjectionEntity projectionEntity = MetadataUtil.generateProjectionEntity(entity);
        projectionMix.getEntities().put(entityName, projectionEntity);
      }
    });

    var globalData = new HashMap<String, Object>();
    globalData.put("entities", projectionMix.getEntitiesMap());
    globalData.put("repositories", moduleMetadata.getRepositoriesMap());
    globalData.put("computedColums", computedColumns);
    globalData.put("includeViews", includeViews);
    globalData.put("javaPackage", modulePackageName);

    Writer outWriterProjection = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8));
    TemplateUtil.processTemplate(template, globalData, outWriterProjection);
  }

  /**
   * Generates the source files for the given projections.
   *
   * @param pathEtendoRx
   * @param projection
   * @param repositories
   * @param computedColumns
   * @param includeViews
   * @param sourcefilePath
   * @param templatePath
   * @param prefix
   * @param sufix
   * @throws FileNotFoundException
   */
  private void generateSourcefile(String pathEtendoRx, Projection projection,
      List<HashMap<String, Object>> repositories, boolean computedColumns, boolean includeViews,
      String sourcefilePath, String templatePath, String prefix, String sufix)
      throws FileNotFoundException {
    generateSourcefile(pathEtendoRx, projection, repositories, computedColumns, includeViews,
        sourcefilePath, templatePath, prefix, sufix, null);
  }

  /**
   * Generates the source files for the given projections.
   *
   * @param pathEtendoRx
   * @param projection
   * @param repositories
   * @param computedColumns
   * @param includeViews
   * @param sourcefilePath
   * @param templatePath
   * @param prefix
   * @param sufix
   * @param packageName
   * @throws FileNotFoundException
   */
  private void generateSourcefile(String pathEtendoRx, Projection projection,
      List<HashMap<String, Object>> repositories, boolean computedColumns, boolean includeViews,
      String sourcefilePath, String templatePath, String prefix, String sufix, String packageName)
      throws FileNotFoundException {

    var outFileDir = pathEtendoRx + sourcefilePath;
    new File(outFileDir).mkdirs();

    String ftlFile = templatePath;
    Template template = TemplateUtil.createTemplateImplementation(ftlFile);

    repositories.forEach(MetadataUtil.HandlingConsumer.handlingConsumerBuilder(repository -> {
      if (projection.getEntities().size() > 0) {
        var entity = projection.getEntities()
            .values()
            .stream()
            .filter(e -> e.getName().compareTo(repository.get("name").toString()) == 0)
            .findFirst();
        entity.ifPresent(MetadataUtil.HandlingConsumer.handlingConsumerBuilder(projectionEntity -> {
          File outFile = null;
          try {
            outFile = new File(outFileDir, repository.get("name").toString() + sufix + ".java");
            repository.put("fields", projectionEntity.getFieldsMap());
            StringBuilder pgkName = new StringBuilder();
            if (packageName != null) {
              pgkName.append(packageName)
                  .append(".")
                  .append(projectionEntity.getPackageName()
                      .replace("com.etendorx.entities.entities.", ""));
            } else {
              pgkName.append(projectionEntity.getPackageName());
            }
            repository.put("packageName", pgkName.toString());
            repository.put("className", projectionEntity.getClassName());
            repository.put("projectionName", projection.getName());
            Writer outWriterProjection = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8));
            TemplateUtil.processTemplate(template, repository, outWriterProjection);
          } catch (IOException e) {
            throw new CodeGenerationException("Cannot create file " + outFile.getAbsolutePath());
          }
        }));
      }
    }));

  }

  /**
   * Generates the source files for the given projections.
   *
   * @param pathEtendoRx
   * @param projection
   * @param repositories
   * @param computedColumns
   * @param includeViews
   * @throws FileNotFoundException
   */
  private void generateGrpcService(String pathEtendoRx, Projection projection,
      List<HashMap<String, Object>> repositories, boolean computedColumns, boolean includeViews)
      throws FileNotFoundException {

    generateSourcefile(pathEtendoRx, projection, repositories, computedColumns, includeViews,
        "/modules_core/com.etendorx.das/src-gen/main/java/com/etendorx/das/grpcrepo",
        "/org/openbravo/base/process/grpcservice.ftl", "", "GrpcService");

  }

  /**
   * Generates the DTO source files for the given projections.
   *
   * @param pathEtendoRx
   * @param projection
   * @param repositories
   * @param computedColumns
   * @param includeViews
   * @throws FileNotFoundException
   */
  private void generateGRPCDto(String pathEtendoRx, Projection projection,
      List<HashMap<String, Object>> repositories, boolean computedColumns, boolean includeViews)
      throws FileNotFoundException {

    generateSourcefile(pathEtendoRx, projection, repositories, computedColumns, includeViews,
        "/modules/com.etendorx.integration.mobilesync/src-gen/main/java/com/etendorx/integration/mobilesync/dto",
        "/org/openbravo/base/process/grpcentitydto.ftl", "", "DTO");

  }

  /**
   * Generates the DTO projection source files for the given projections.
   *
   * @param pathEtendoRx
   * @param projection
   * @param repositories
   * @param computedColumns
   * @param includeViews
   * @throws FileNotFoundException
   */
  private void generateGRPCDtoProjection(String pathEtendoRx, Projection projection,
      List<HashMap<String, Object>> repositories, boolean computedColumns, boolean includeViews)
      throws FileNotFoundException {

    generateSourcefile(pathEtendoRx, projection, repositories, computedColumns, includeViews,
        "/modules/com.etendorx.integration.mobilesync/src-gen/main/java/com/etendorx/integration/mobilesync/dto",
        "/org/openbravo/base/process/entitydtogrpc2model.ftl", "",
        "DTOGrpc2" + projection.getName().substring(0, 1).toUpperCase() + projection.getName()
            .substring(1), "com.etendorx.integration.mobilesync.entities"

    );
  }

  /**
   * Generates the DTO projection source files for the given projections.
   *
   * @param pathEtendoRx
   * @param projection
   * @param repositories
   * @param computedColumns
   * @param includeViews
   * @throws FileNotFoundException
   */
  private void generateProjectionDTO2Grpc(String pathEtendoRx, Projection projection,
      List<HashMap<String, Object>> repositories, boolean computedColumns, boolean includeViews)
      throws FileNotFoundException {

    generateSourcefile(pathEtendoRx, projection, repositories, computedColumns, includeViews,
        "/modules/com.etendorx.integration.mobilesync/src-gen/main/java/com/etendorx/integration/mobilesync/dto",
        "/org/openbravo/base/process/entitydtoprojection2grpc.ftl", "",
        "DTO" + projection.getName().substring(0, 1).toUpperCase() + projection.getName()
            .substring(1) + "2Grpc", "com.etendorx.integration.mobilesync.entities");

  }

  /**
   * Generates the client service source files for the given projections.
   *
   * @param pathEtendoRx
   * @param projection
   * @param repositories
   * @param computedColumns
   * @param includeViews
   * @throws FileNotFoundException
   */
  private void generateClientGrpcService(String pathEtendoRx, Projection projection,
      List<HashMap<String, Object>> repositories, boolean computedColumns, boolean includeViews)
      throws FileNotFoundException {

    generateSourcefile(pathEtendoRx, projection, repositories, computedColumns, includeViews,
        "/modules/com.etendorx.integration.mobilesync/src-gen/main/java/com/etendorx/integration/mobilesync/service/",
        "/org/openbravo/base/process/grpcclientservice.ftl", "",
        "" + projection.getName().substring(0, 1).toUpperCase() + projection.getName()
            .substring(1) + "DasServiceGrpcImpl", "com.etendorx.integration.mobilesync.entities");

  }

  /**
   * Generates the client service interface source files for the given projections.
   *
   * @param pathEtendoRx
   * @param projection
   * @param repositories
   * @param computedColumns
   * @param includeViews
   * @throws FileNotFoundException
   */
  private void generateClientServiceInterface(String pathEtendoRx, Projection projection,
      List<HashMap<String, Object>> repositories, boolean computedColumns, boolean includeViews)
      throws FileNotFoundException {

    generateSourcefile(pathEtendoRx, projection, repositories, computedColumns, includeViews,
        "/modules/com.etendorx.integration.mobilesync/src-gen/main/java/com/etendorx/integration/mobilesync/service/",
        "/org/openbravo/base/process/grpcclientinterface.ftl", "",
        projection.getName().substring(0, 1).toUpperCase() + projection.getName()
            .substring(1) + "DasService", "com.etendorx.integration.mobilesync.entities");

  }

  public void setEntitiesModel(List<Entity> entitiesModel) {
    this.entitiesModel = entitiesModel;
    this.setEntitiesModelMap(MetadataUtil.generateEntitiesMap(entitiesModel));
  }

  public void setEntitiesModelMap(Map<String, Entity> entitiesModelMap) {
    this.entitiesModelMap = entitiesModelMap;
  }

}
