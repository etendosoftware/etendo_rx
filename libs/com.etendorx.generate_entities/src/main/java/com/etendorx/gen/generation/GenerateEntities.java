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

import com.etendoerp.etendorx.model.ETRXModelProvider;
import com.etendorx.gen.beans.Metadata;
import com.etendorx.gen.beans.Projection;
import com.etendorx.gen.beans.Repository;
import com.etendorx.gen.commandline.CommandLineProcess;
import com.etendorx.gen.generation.interfaces.EntityGenerator;
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
   * @param cmdProcess the command line process
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

    log.info("Generate Etendo Rx Code={}", generateRxCode);
    log.info("Path Project Rx={}", pathEtendoRx);
    log.info("Test entitites {}", cmdProcess.isTest());

    List<Entity> entities = ModelProvider.getInstance().getModel();

    var projections = getProjections(paths, entities);
    var generators = getGenerators(projections);

    try {
      for (Entity entity : entities) {
        if (entity.isDataSourceBased() || entity.isHQLBased()) {
          continue;
        }
        if (generateRxCode && !entity.isVirtualEntity() && (includeViews || !entity.isView())) {
          var data = TemplateUtil.getModelData(paths, entity, getSearchesMap(entity), computedColumns, includeViews);
          for (EntityGenerator generator : generators) {
            generator.generate(data, paths);
          }
        }
      }
      generateEntityScan(entities, paths.pathEntitiesRx);
      generateBaseEntityRx(new HashMap<>(), paths.pathEntitiesRx, paths.baseRXObject, paths.packageEntities);
      generateProtofile(projections, entities, paths, computedColumns, includeViews);

    } catch (IOException e) {
      log.error(ERROR_GENERATING_FILE + GENERATED_DIR, e);
    }
    log.info("Generated {} entities", entities.size());
  }

  /**
   * Generates the projections
   * @param paths the paths
   * @param entities the entities
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
   * @param projections the projections
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
   * @param projections the projections
   * @param entities the entities
   * @param paths the paths
   * @param computedColumns the computed columns
   * @param includeViews the include views
   * @throws FileNotFoundException the file not found exception
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
   * @param entities the entities
   * @return the generated projections
   */
  private Projection getDefaultProjection(List<Entity> entities) {
    var projection = new Projection(PROJECTION_DEFAULT);
    new ProjectionsConverter().convert(projection, entities);
    return projection;
  }

  /**
   * Get the search map
   * @param entity the entity
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
   * @param entity the entity
   * @return the repository list
   */
  private List<Repository> getRepositories(Entity entity) {
    return new RepositoriesConverter().convert(
        ETRXModelProvider.getInstance().getETRXRepositories(entity)
    );
  }

  /**
   * Get repositories
   * @return the repository list
   */
  private List<Repository> getRepositories() {
    return new RepositoriesConverter().convert(
        ETRXModelProvider.getInstance().getETRXRepositories()
    );
  }

  /**
   * Get projections
   * @param paths the paths
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
   * @param entities the entities
   * @param pathEntitiesRx the path entities rx
   * @throws FileNotFoundException
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
   * Generates the base entity rx
   * @param data the data
   * @param pathEntitiesRx the path entities rx
   * @param className the class name
   * @param packageName the package name
   * @throws FileNotFoundException the file not found exception
   */
  private void generateBaseEntityRx(Map<String, Object> data, String pathEntitiesRx, String className,
      String packageName) throws FileNotFoundException {
    final String fullPathEntities = pathEntitiesRx + "/src/main/entities/" + packageName.replace(".", "/");
    var classfileName = className + ".java";
    var outFile = new File(fullPathEntities, classfileName);
    new File(outFile.getParent()).mkdirs();
    String ftlFileNameRX = "/org/openbravo/base/gen/baseEntityRx.ftl";
    freemarker.template.Template templateRX = TemplateUtil.createTemplateImplementation(
        ftlFileNameRX);
    Writer outWriter = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8));
    TemplateUtil.processTemplate(templateRX, data, outWriter);
  }

}
