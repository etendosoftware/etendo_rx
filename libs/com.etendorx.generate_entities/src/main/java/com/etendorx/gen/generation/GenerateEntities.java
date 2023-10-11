/*
 * Copyright 2022-2023  Futit Services SL
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
package com.etendorx.gen.generation;

import static com.etendorx.gen.generation.GenerateEntitiesConstants.PROJECTION_DEFAULT;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.etendorx.base.session.OBPropertiesProvider;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Table;

import com.etendoerp.etendorx.model.ETRXModelProvider;
import com.etendoerp.etendorx.model.projection.ETRXProjectionEntity;
import com.etendorx.gen.beans.Metadata;
import com.etendorx.gen.beans.Projection;
import com.etendorx.gen.beans.Repository;
import com.etendorx.gen.commandline.CommandLineProcess;
import com.etendorx.gen.generation.interfaces.EntityGenerator;
import com.etendorx.gen.generation.interfaces.MappingGenerator;
import com.etendorx.gen.generation.mapping.GenerateBaseDTO;
import com.etendorx.gen.generation.mapping.GenerateBaseDTOConverter;
import com.etendorx.gen.generation.mapping.GenerateBaseFieldConverterRead;
import com.etendorx.gen.generation.mapping.GenerateBaseFieldConverterWrite;
import com.etendorx.gen.generation.mapping.GenerateBaseJsonPathConverter;
import com.etendorx.gen.generation.mapping.GenerateBaseJsonPathRetriever;
import com.etendorx.gen.generation.mapping.GenerateBaseRepository;
import com.etendorx.gen.generation.mapping.GenerateBaseRestController;
import com.etendorx.gen.metadata.MetadataContainer;
import com.etendorx.gen.process.GenerateProtoFile;
import com.etendorx.gen.util.TemplateUtil;

public class GenerateEntities {
  public static final String ERROR_GENERATING_FILE = "Error generating file: ";
  public static final String GENERATING_FILE = "Generating file: ";
  public static final String MODULES_GEN = "modules_gen";
  public final static String GENERATED_DIR = "/../build/tmp/generated";
  private static final Logger log = LogManager.getLogger();
  private String basePath;
  private String propertiesFile;

  public String getBasePath() {
    return basePath;
  }

  public void setBasePath(String basePath) {
    this.basePath = basePath;
  }

  public void setFriendlyWarnings(boolean doFriendlyWarnings) {
    OBPropertiesProvider.setFriendlyWarnings(doFriendlyWarnings);
  }

  public String getPropertiesFile() {
    return propertiesFile;
  }

  public void setPropertiesFile(String propertiesFile) {
    this.propertiesFile = propertiesFile;
  }

  /**
   * Executes the command
   *
   * @param cmdProcess
   *     the command line process
   */
  public void execute(CommandLineProcess cmdProcess) {
    if (getBasePath() == null) {
      setBasePath(".");
    }
    log.debug("initializating dal layer, getting properties from {}", getPropertiesFile());
    OBPropertiesProvider.getInstance().setProperties(getPropertiesFile());

    final Properties obProperties = OBPropertiesProvider.getInstance().getOpenbravoProperties();

    String pathEtendoRx = obProperties.getProperty("rx.path");

    if (pathEtendoRx == null || pathEtendoRx.isBlank()) {
      pathEtendoRx = ".";
    }

    var paths = new GeneratePaths(pathEtendoRx);

    final boolean generateRxCode = Boolean.parseBoolean(obProperties.getProperty("rx.generateCode"));
    final boolean computedColumns = Boolean.parseBoolean(obProperties.getProperty("rx.computedColumns"));
    final boolean includeViews = Boolean.parseBoolean(obProperties.getProperty("rx.views"));
    final boolean dataRestEnabled = Boolean.parseBoolean(obProperties.getProperty("data-rest.enabled"));

    log.info("Generate Etendo Rx Code={}", generateRxCode);
    log.info("Path Project Rx={}", pathEtendoRx);
    log.info("Test entitites {}", cmdProcess.isTest());

    List<Entity> entities = ModelProvider.getInstance().getModel();

    var projections = getProjections(paths, entities);
    var generators = getGenerators(projections);
    var mappingGenerators = getMappingGenerators();
    try {
      for (Entity entity : entities) {
        if (entity.isDataSourceBased() || entity.isHQLBased()) {
          continue;
        }
        if (generateRxCode && !entity.isVirtualEntity() && (includeViews || !entity.isView())) {
          var data = TemplateUtil.getModelData(paths, entity, getSearchesMap(entity), computedColumns, includeViews);
          generateEntityCode(data, paths, generators, dataRestEnabled);
          generateMappingCode(entity, paths, mappingGenerators);
        }
      }

      generateGlobalCode(paths, entities);

      generateProtofile(projections, entities, paths, computedColumns, includeViews);

    } catch (IOException e) {
      log.error(ERROR_GENERATING_FILE + GENERATED_DIR, e);
    }
    log.info("Generated {} entities", entities.size());
  }

  private void generateGlobalCode(GeneratePaths paths, List<Entity> entities) throws FileNotFoundException {
    generateEntityScan(entities, paths.pathEntitiesRx);

    generateBaseEntityRx(new HashMap<>(), paths.pathEntitiesRx, paths.baseSerializableObject, Templates.BASE_SERIALIZABLE_OBJECT_FTL,
        paths.packageEntities);
    generateBaseEntityRx(new HashMap<>(), paths.pathEntitiesRx, paths.baseRXObject, Templates.BASE_ENTITY_RX_FTL,
        paths.packageEntities);
    generateBaseEntityRx(new HashMap<>(), paths.pathEntitiesRx, paths.baseDASRepository, Templates.BASE_DASREPOSITORY_FTL,
        paths.packageEntities);
    generateBaseEntityRx(new HashMap<>(), paths.pathEntitiesRx, paths.baseDTORepository, Templates.BASE_DTOREPOSITORY_FTL,
        paths.packageEntities);
    generateBaseEntityRx(new HashMap<>(), paths.pathEntitiesRx, paths.mappingUtils, Templates.MAPPING_UTILS_FTL,
        paths.packageEntities + ".mappings");
    generateBaseEntityRx(new HashMap<>(), paths.pathEntitiesRx, paths.auditServiceInterceptor, Templates.AUDIT_SERVICE_INTERCEPTOR_FTL,
        paths.packageEntities);
  }

  private List<MappingGenerator> getMappingGenerators() {
    var mappingGenerators = new ArrayList<MappingGenerator>();
    mappingGenerators.add(new GenerateBaseDTO());
    mappingGenerators.add(new GenerateBaseFieldConverterRead());
    mappingGenerators.add(new GenerateBaseFieldConverterWrite());
    mappingGenerators.add(new GenerateBaseJsonPathConverter());
    mappingGenerators.add(new GenerateBaseJsonPathRetriever());
    mappingGenerators.add(new GenerateBaseRestController());
    mappingGenerators.add(new GenerateBaseRepository());
    return mappingGenerators;
  }

  private void generateEntityCode(Map<String, Object> data, GeneratePaths paths,
      List<EntityGenerator> generators, boolean dataRestEnabled) throws FileNotFoundException {
    for (EntityGenerator generator : generators) {
      generator.generate(data, paths, dataRestEnabled);
    }
  }

  private void generateMappingCode(Entity entity, GeneratePaths paths, List<MappingGenerator> mappingGenerators) throws FileNotFoundException {
    // Mappings
    Table table = ModelProvider.getInstance().getTable(entity.getTableName());
    List<ETRXProjectionEntity> list = ETRXModelProvider.getInstance().getETRXProjectionEntity(table);
    new GenerateBaseDTOConverter().generate(list, paths);
    // mappings
    for (ETRXProjectionEntity etrxProjectionEntity : list) {
      if (hasReadWrite(list, etrxProjectionEntity)) {
        for (MappingGenerator mappingGenerator : mappingGenerators) {
          mappingGenerator.generate(etrxProjectionEntity, paths);
        }
      }
    }
  }

  private boolean hasReadWrite(List<ETRXProjectionEntity> list, ETRXProjectionEntity entity) {
    return list.stream()
        .filter(m -> m.getProjection().getId().equals(entity.getProjection().getId()))
        .count() == 2;
  }

  /**
   * Generates the projections
   *
   * @param paths
   *     the paths
   * @param entities
   *     the entities
   *
   * @return the projections
   */
  private ArrayList<Projection> getProjections(GeneratePaths paths, List<Entity> entities) {
    var projections = new ArrayList<Projection>();
    var projectionDefault = getDefaultProjection(entities);
    projections.add(projectionDefault);
    projections.addAll(getProjections(paths));
    return projections;
  }

  /**
   * Generates the generators
   *
   * @param projections
   *     the projections
   *
   * @return the generators
   */
  private List<EntityGenerator> getGenerators(ArrayList<Projection> projections) {
    List<EntityGenerator> generators = new ArrayList<>();
    generators.add(new GenerateEntityRX());
    generators.add(new GenerateJPARepo());
    generators.add(new GenerateClientRestRX());
    generators.add(new GenerateEntityModel());
    generators.add(new GenerateProjections(projections));
    return generators;
  }

  /**
   * Generates the protofile
   *
   * @param projections
   *     the projections
   * @param entities
   *     the entities
   * @param paths
   *     the paths
   * @param computedColumns
   *     the computed columns
   * @param includeViews
   *     the include views
   *
   * @exception FileNotFoundException
   *     the file not found exception
   */
  private void generateProtofile(ArrayList<Projection> projections, List<Entity> entities, GeneratePaths paths,
      boolean computedColumns, boolean includeViews) throws FileNotFoundException {
    List<Repository> repositories = new ArrayList<>();
    repositories.addAll(getRepositories());

    MetadataContainer metadataContainer = new MetadataContainer();
    Metadata metadata = new Metadata();
    for (Repository repository : repositories) {
      metadata.getRepositories().put(repository.getEntityName(), repository);
    }
    for (Projection projection : projections) {
      metadata.getProjections().put(projection.getName(), projection);
    }
    metadataContainer.getMetadataList().add(metadata);
    GenerateProtoFile generateProtoFile = new GenerateProtoFile();
    generateProtoFile.setEntitiesModel(entities);
    // Generate Proto File
    generateProtoFile.generate(paths.pathEtendoRx, metadata.getRepositoriesMap(), projections, metadataContainer,
        computedColumns, includeViews);

  }

  /**
   * Gets the default projection
   *
   * @param entities
   *     the entities
   *
   * @return the generated projections
   */
  private Projection getDefaultProjection(List<Entity> entities) {
    var projection = new Projection(PROJECTION_DEFAULT);
    new ProjectionsConverter().convert(projection, entities);
    return projection;
  }

  /**
   * Get the search map
   *
   * @param entity
   *     the entity
   *
   * @return the search map
   */
  private List<HashMap<String, Object>> getSearchesMap(Entity entity) {
    List<Repository> repositories = getRepositories(entity);
    List<HashMap<String, Object>> searchesMap = new ArrayList<>();
    for (Repository repository : repositories) {
      searchesMap.addAll(repository.getSearchesMap());
    }
    return searchesMap;
  }

  /**
   * Get repositories from entity
   *
   * @param entity
   *     the entity
   *
   * @return the repository list
   */
  private List<Repository> getRepositories(Entity entity) {
    return new RepositoriesConverter().convert(
        ETRXModelProvider.getInstance().getETRXRepositories(entity)
    );
  }

  /**
   * Get repositories
   *
   * @return the repository list
   */
  private List<Repository> getRepositories() {
    return new RepositoriesConverter().convert(
        ETRXModelProvider.getInstance().getETRXRepositories()
    );
  }

  /**
   * Get projections
   *
   * @param paths
   *     the paths
   *
   * @return the projections
   */
  private List<Projection> getProjections(GeneratePaths paths) {
    return new ProjectionsConverter().convert(
        paths,
        ETRXModelProvider.getInstance().getETRXProjection()
    );
  }

  /**
   * Generates the entity scan
   *
   * @param entities
   *     the entities
   * @param pathEntitiesRx
   *     the path entities rx
   */
  private void generateEntityScan(List<Entity> entities, String pathEntitiesRx) throws FileNotFoundException {
    Map<String, Object> data = new HashMap<>();
    data.put("packages", entities.stream().map(Entity::getPackageName)
        .distinct()
        .collect(Collectors.toList()));
    var outFile = new File(pathEntitiesRx, "src/main/entities/com/etendorx/das/scan/EntityScan.java");
    new File(outFile.getParent()).mkdirs();
    String ftlFileNameRX = "/org/openbravo/base/gen/entityscan.ftl";
    freemarker.template.Template templateRX = TemplateUtil.createTemplateImplementation(
        ftlFileNameRX);
    Writer outWriter = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8));
    TemplateUtil.processTemplate(templateRX, data, outWriter);
  }

  /**
   * Generates the baseRestController.ftl entity rx
   *
   * @param data
   *     the data
   * @param pathEntitiesRx
   *     the path entities rx
   * @param className
   *     the class name
   * @param packageName
   *     the package name
   *
   * @exception FileNotFoundException
   *     the file not found exception
   */
  private void generateBaseEntityRx(Map<String, Object> data, String pathEntitiesRx, String className, String template,
      String packageName) throws FileNotFoundException {
    final String fullPathEntities = pathEntitiesRx + "/src/main/entities/" + packageName.replace(".", "/");
    var classfileName = className + ".java";
    var outFile = new File(fullPathEntities, classfileName);
    new File(outFile.getParent()).mkdirs();
    String ftlFileNameRX = "/org/openbravo/base/gen/" + template;
    freemarker.template.Template templateRX = TemplateUtil.createTemplateImplementation(
        ftlFileNameRX);
    Writer outWriter = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8));
    TemplateUtil.processTemplate(templateRX, data, outWriter);
  }

}
