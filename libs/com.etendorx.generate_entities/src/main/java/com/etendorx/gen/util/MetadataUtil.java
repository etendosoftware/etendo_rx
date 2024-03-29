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
package com.etendorx.gen.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.etendorx.base.exception.OBException;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Util class needed for code generation
 *
 * @author Sebastian Barrozo
 */

public class MetadataUtil {
  private static final Logger log = LogManager.getLogger();
  public static final String REACT = "react";
  public static final String REPOSITORIES = "repositories";
  public static final String PROJECTIONS = "projections";
  public static final String ENTITIES = "entities";
  public static final String NAME = "name";
  public static final String FIELDS = "fields";
  private static final String ENTITY_NAME = "entityName";
  private static final String SEARCHES = "searches";
  private static final String QUERY = "query";
  private static final String METHOD = "method";
  private static final String PARAMS = "params";
  private static final String TYPE = "type";
  private static final String VALUE = "value";
  public static final String TRANSACTIONAL = "transactional";
  public static final String GRPC = "grpc";
  public static final String IDENTITY = "identity";
  public static final String FETCH_ATTRIBUTES = "fetchAttributes";


  /**
   * Name of the packages used to store the autogenerated classes in each module
   */
  public static final String ENTITY_PACKAGE = "entities";
  public static final String CLIENTREST_PACKAGE = "clientrest";

  public static MetadataContainer analyzeMetadata(String pathEtendoRx, boolean testFiles) throws OBException {

    MetadataContainer metadataContainer = new MetadataContainer();
    var metadata = new Metadata();
    metadataContainer.setMetadataMix(metadata);

    var modulesDir = new ArrayList<>(List.of("modules", "modules_core", "../modules_rx"));
    if (testFiles) {
      modulesDir.add("modules_test");
    }

    List<File> directories = new ArrayList<>();
    modulesDir.forEach(dir -> {
      log.info("Search projections in path {}/{}", pathEtendoRx, dir);
      directories.addAll(getMetadataFiles(pathEtendoRx + File.separator + dir + File.separator));
    });

    for (File dir : directories) {
      var metadataPath = dir + File.separator + "src-db" + File.separator + "das" + File.separator + "metadata.json";
      var projectionsFile = new File(metadataPath);
      if (projectionsFile.exists()) {
        log.info("Founded metadata {}", metadataPath);

        Metadata moduleMetadata = new Metadata();
        moduleMetadata.setLocationModule(dir);
        metadataContainer.getMetadataList().add(moduleMetadata);

        try {
          analizeProjections(metadata.getProjections(), projectionsFile, dir, moduleMetadata);
          analizeRepositories(metadata.getRepositories(), projectionsFile, moduleMetadata);
        } catch (JSONException | IOException | CodeGenerationException exception) {
          log.error(exception);
          throw new OBException(exception.getMessage());
        }
      }
    }
    return metadataContainer;
  }

  /**
   * Obtains the absolute path of the base package where all the java files will be created.
   * Ex: given the module 'com.test.mymodule'
   * returns 'module.absolutePath + /src-gen/main/java/com/test/mymodule'
   *
   * @param moduleLocation
   */
  public static String getBasePackageGenLocationPath(File moduleLocation) {
    return getSrcGenLocation(moduleLocation) + File.separator + "/main/java" + File.separator + getBasePackageGenPath(moduleLocation);
  }

  /**
   * Obtains the absolute path where all generated files will be stored
   *
   * @param moduleLocation
   */
  public static String getSrcGenLocation(File moduleLocation) {
    return moduleLocation.getAbsolutePath() + File.separator + "src-gen";
  }

  /**
   * Parses the name of the module to a package path.
   * Ex: 'com.etendorx.mymodule' -> 'com/etendorx/mymodule'
   *
   * @param moduleLocation
   */
  public static String getBasePackageGenPath(File moduleLocation) {
    return moduleLocation.getName().replace("\\.", "/");
  }

  /**
   * Generates a Map between the 'newClassname' (the name of the {@link ProjectionEntity} defined in a projection) and the corresponding {@link Entity}.
   *
   * @param entities
   *   List of entities to map
   *
   * @return Map
   */
  public static Map<String, Entity> generateEntitiesMap(List<Entity> entities) {
    Map<String, Entity> entityMap = new HashMap<>();
    entities.forEach(entity -> {
      String newClassName = entity.getName();
      entityMap.put(newClassName, entity);
    });
    return entityMap;
  }

  /**
   * Creates a new {@link ProjectionEntity} based on a {@link Entity}
   *
   * @param entityModel
   *   {@link Entity}
   *
   * @return {@link ProjectionEntity}
   */
  public static ProjectionEntity generateProjectionEntity(Entity entityModel) {
    String newClassName = entityModel.getName();
    ProjectionEntity projectionEntity = new ProjectionEntity(newClassName, "");

    // Filter the valid properties of the entityModel
    var filteredProperties = entityModel.getProperties().stream().filter(property ->
      !property.isComputedColumn() &&
        !StringUtils.isBlank(property.getTypeName())
        && (property.isId() || (property.isPrimitive() && !property.getPrimitiveType().isArray())
        || (property.getTargetEntity() != null && !property.isOneToMany() && !property.getTargetEntity().isView()))
    );

    // Fill the projectionEntity fields with the entityModel filteredProperties
    filteredProperties.forEach(property -> {
      ProjectionEntityField projectionEntityField = generateProjectionEntityField(property);
      projectionEntity.getFields().put(projectionEntityField.getName(), projectionEntityField);
    });

    return projectionEntity;
  }

  /**
   * Creates a new {@link ProjectionEntityField} based on a {@link Property}
   *
   * @param propertyModel
   *   {@link Property}
   *
   * @return {@link ProjectionEntityField}
   */
  public static ProjectionEntityField generateProjectionEntityField(Property propertyModel) {
    return new ProjectionEntityField(propertyModel.getName(), null, propertyModel.getTypeName(), generateClassName(propertyModel));
  }

  static String generateClassName(Property propertyModel) {
    String className = "";
    if (propertyModel.getTargetEntity() != null && propertyModel.getTargetEntity().getName() != null) {
      var tableNameSplit = propertyModel.getTargetEntity().getTableName().split("_");
      var cn = "";
      for (String s : tableNameSplit) {
        cn += s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
      }
      className = cn;
    }
    return className;
  }

  public static List<File> getMetadataFiles(String path) {
    File location = new File(path);

    if (!location.exists()) {
      log.info("The location to search Metadata files '" + path + "' does not exists.");
      return new ArrayList<>();
    }

    return Arrays.stream(Objects.requireNonNull(new File(path).listFiles(File::isDirectory))).collect(
      Collectors.toList());
  }

  private static void analizeProjections(Map<String, Projection> projections, File projectionsFile, File moduleLocation, Metadata moduleMetadata)
    throws IOException, JSONException, CodeGenerationException {
    var content = Files.readString(projectionsFile.toPath());
    var jsonProps = new JSONObject(content);
    var react = false;
    if (jsonProps.has(REACT) && !jsonProps.isNull(REACT)) {
      react = jsonProps.getBoolean(REACT);
    }
    if (jsonProps.has(PROJECTIONS)) {
      var projs = jsonProps.getJSONArray(PROJECTIONS);
      for (var i = 0; i < projs.length(); i++) {
        var jsonProjection = projs.getJSONObject(i);
        var name = jsonProjection.getString(NAME);
        boolean grpc = false;
        if (jsonProjection.has(GRPC)) {
          grpc = jsonProjection.getBoolean(GRPC);
        }
        if (jsonProjection.has(REACT)) {
          react = jsonProjection.getBoolean(REACT);
        }
        // TODO: If different modules use the same name of a projection, then the location module will be incorrect.

        Projection moduleProjection = getProjection(moduleMetadata.getProjections(), name, grpc, react);

        Projection projection = getProjection(projections, name, grpc, react);
        projection.setModuleLocation(moduleLocation);
        if (jsonProjection.has(ENTITIES)) {
          var entities = jsonProjection.getJSONArray(ENTITIES);
          for (var j = 0; j < entities.length(); j++) {
            var jsonEntity = entities.getJSONObject(j);
            log.debug("Analyzing projectionEntity {}", jsonEntity);
            var entityName = jsonEntity.getString(NAME);
            String identity = null;
            if (jsonEntity.has(IDENTITY)) {
              identity = jsonEntity.getString(IDENTITY);
            }

            ProjectionEntity moduleProjectionEntity = getProjectionEntity(moduleProjection, entityName, identity);

            ProjectionEntity projectionEntity = getProjectionEntity(projection, entityName, identity);
            if (jsonEntity.has(FIELDS)) {
              var fields = jsonEntity.getJSONArray(FIELDS);
              log.debug("Analyzing fields {}", fields);
              for (var k = 0; k < fields.length(); k++) {
                var field = fields.getJSONObject(k);
                if (field.has(NAME)) {
                  var fieldName = field.getString(NAME);
                  String fieldValue = null;
                  if (field.has(VALUE)) {
                    fieldValue = field.getString(VALUE);
                  }
                  String fieldType = null;
                  if (field.has(TYPE)) {
                    fieldType = field.getString(TYPE);
                  }
                  getProjectionEntityField(moduleProjectionEntity, fieldName, fieldValue, fieldType);
                  getProjectionEntityField(projectionEntity, fieldName, fieldValue, fieldType);
                }
              }
            } else {
              throw new CodeGenerationException("The entity '" + entityName + "' doesn't have any fields ");
            }
          }
        }
      }
    }
  }

  private static void analizeRepositories(Map<String, Repository> repositories,
                                          File projectionsFile, Metadata moduleMetadata) throws IOException, JSONException {
    var content = Files.readString(projectionsFile.toPath());
    var jsonProps = new JSONObject(content);
    if (jsonProps.has(REPOSITORIES)) {
      var projs = jsonProps.getJSONArray(REPOSITORIES);
      for (var i = 0; i < projs.length(); i++) {
        var jsonRepository = projs.getJSONObject(i);
        var entityName = jsonRepository.getString(ENTITY_NAME);
        Boolean transactional = false;
        if (jsonRepository.has(TRANSACTIONAL)) {
          transactional = jsonRepository.getBoolean(TRANSACTIONAL);
        }

        Repository moduleRepository = getRepository(moduleMetadata.getRepositories(), entityName, transactional);

        Repository repository = getRepository(repositories, entityName, transactional);
        if (jsonRepository.has(SEARCHES)) {
          var searches = jsonRepository.getJSONArray(SEARCHES);
          for (var j = 0; j < searches.length(); j++) {
            var jsonSearch = searches.getJSONObject(j);
            log.info("Analyzing repositorySearch {}", jsonSearch);
            RepositorySearch repositorySearch;
            var query = jsonSearch.getString(QUERY);
            var method = jsonSearch.getString(METHOD);
            JSONArray fetchAttributes = null;
            if (jsonSearch.has(FETCH_ATTRIBUTES)) {
              fetchAttributes = jsonSearch.getJSONArray(FETCH_ATTRIBUTES);
            }

            RepositorySearch moduleRepositorySearch = getRepositorySearch(moduleRepository, method, query, fetchAttributes);

            repositorySearch = getRepositorySearch(repository, method, query, fetchAttributes);
            if (jsonSearch.has(PARAMS)) {
              var params = jsonSearch.getJSONArray(PARAMS);
              log.info("Analyzing params {}", params);
              for (var k = 0; k < params.length(); k++) {
                var field = params.getJSONObject(k);
                if (field.has(NAME)) {
                  var paramName = field.getString(NAME);
                  var paramType = field.getString(TYPE);
                  getRepositorySearchParam(moduleRepositorySearch, paramName, paramType);
                  getRepositorySearchParam(repositorySearch, paramName, paramType);
                }
              }
            }
          }
        }
      }
    }
  }

  private static ProjectionEntityField getProjectionEntityField(ProjectionEntity projectionEntity,
                                                                String fieldName, String fieldValue, String fieldType) {
    ProjectionEntityField projectionEntityField;
    if (projectionEntity.getFields().containsKey(fieldName)) {
      projectionEntityField = projectionEntity.getFields().get(fieldName);
    } else {
      projectionEntityField = new ProjectionEntityField(fieldName, fieldValue, fieldType);
      projectionEntity.getFields().put(fieldName, projectionEntityField);
    }
    return projectionEntityField;
  }

  private static ProjectionEntity getProjectionEntity(Projection projection, String entityName, String identity) {
    ProjectionEntity projectionEntity;
    if (projection.getEntities().containsKey(entityName)) {
      projectionEntity = projection.getEntities().get(entityName);
    } else {
      projectionEntity = new ProjectionEntity(entityName, identity);
      projection.getEntities().put(entityName, projectionEntity);
    }
    return projectionEntity;
  }

  /**
   * Generates a unique projection from a module metadata.
   * The projections contain a merge from multiple projections with the same entity.
   * The entity will contain all the declared fields.
   *
   * @param moduleMetadata
   *   The module from where the projection will be obtained
   * @param projectionFilter
   *   A lambda function used to filter the module projections
   *
   * @return A Projection.
   */
  public static Projection generateProjectionMix(Metadata moduleMetadata, Predicate<Projection> projectionFilter) {
    Projection projectionMix = new Projection("mix");

    List<Projection> moduleProjectionFiltered = moduleMetadata.getProjections().values().stream().filter(projectionFilter).collect(Collectors.toList());
    for (Projection moduleProjection : moduleProjectionFiltered) {
      for (ProjectionEntity moduleProjectionEntity : moduleProjection.getEntities().values()) {
        ProjectionEntity projectionEntityMix = getProjectionEntity(projectionMix, moduleProjectionEntity.getName(), "");
        for (ProjectionEntityField moduleProjectionEntityField : moduleProjectionEntity.getFields().values()) {
          getProjectionEntityField(
            projectionEntityMix,
            moduleProjectionEntityField.getName(),
            moduleProjectionEntityField.getValue(),
            moduleProjectionEntityField.getType()
          );
        }
      }
    }
    return projectionMix;
  }

  private static Projection getProjection(Map<String, Projection> projections, String projectionName, boolean grpc, boolean react) {
    Projection projection;
    if (projections.containsKey(projectionName)) {
      projection = projections.get(projectionName);
    } else {
      projection = new Projection(projectionName, grpc, react);
      projections.put(projectionName, projection);
    }
    return projection;
  }

  private static Repository getRepository(Map<String, Repository> repositories, String entityName,
                                          boolean transactional) {
    Repository repository;
    if (repositories.containsKey(entityName)) {
      repository = repositories.get(entityName);
    } else {
      repository = new Repository(entityName, transactional);
      repositories.put(entityName, repository);
    }
    return repository;
  }

  private static RepositorySearch getRepositorySearch(Repository repository, String method,
                                                      String query, JSONArray fetchAttributes) {
    RepositorySearch repositorySearch;
    if (repository.getSearches().containsKey(method)) {
      repositorySearch = repository.getSearches().get(method);
    } else {
      repositorySearch = new RepositorySearch(method, query, fetchAttributes);
      repository.getSearches().put(method, repositorySearch);
    }
    return repositorySearch;
  }

  private static RepositorySearchParam getRepositorySearchParam(RepositorySearch repository,
                                                                String name, String type) {
    RepositorySearchParam repositorySearchParam;
    if (repository.getSearchParams().containsKey(name)) {
      repositorySearchParam = repository.getSearchParams().get(name);
    } else {
      repositorySearchParam = new RepositorySearchParam(name, type);
      repository.getSearchParams().put(name, repositorySearchParam);
    }
    return repositorySearchParam;
  }

  public static Metadata fillTypes(Metadata metadata, List<Entity> entities, String packageEntities)
    throws CodeGenerationException {
    metadata.getProjections().values()
      .forEach(HandlingConsumer.handlingConsumerBuilder(p -> {
        var withoutFields = p.getEntities().values().stream().filter(e -> e.getFields().size() == 0)
          .collect(Collectors.toList());
        if (withoutFields.size() > 0) {
          throw new CodeGenerationException("Entitiy " + withoutFields.stream().map(
            ProjectionEntity::getName).collect(Collectors.toList()) + " without fields "
          );
        }
      }));

    metadata.getProjections()
      .values()
      .forEach(HandlingConsumer.handlingConsumerBuilder(projection -> projection.getEntities()
        .values()
        .forEach(HandlingConsumer.handlingConsumerBuilder(entity -> entity.getFields()
          .values()
          .forEach(HandlingConsumer.handlingConsumerBuilder(
            field -> manageFillTypes(entities, packageEntities, entity, field)))))));
    return metadata;
  }

  private static Optional<Property> filterEntityPropertyByName(Entity entity, String entityPropertyName) {
    return entity.getProperties()
      .stream()
      .filter(p -> p.getName().compareTo(entityPropertyName) == 0)
      .findFirst();
  }

  /**
   * Verifies if a property of the 'value' declared in a {@link ProjectionEntityField} is valid.
   * <p>
   * Example
   * {
   * "name": "businessPartnerCategoryName",
   * "value": "businessPartner.businessPartnerCategory.name"
   * }
   * <p>
   * The 'businessPartner' should be a property of the declared 'baseEntity'
   * The 'businessPartnerCategory' should be a property of the 'businessPartner'
   * the 'name' should be a property of the 'businessPartnerCategory'
   *
   * @param baseEntity
   * @param baseField
   * @param parentEntity
   * @param propertyName
   * @param isLastValue
   *   boolean Flag used to check if the property is the last in the field 'value'
   *
   * @return A {@link Entity} representing the target entity of the 'propertyName'
   */
  private static Entity validateParentEntityValue(ProjectionEntity baseEntity, ProjectionEntityField baseField, Entity parentEntity, String propertyName, boolean isLastValue) throws CodeGenerationException {
    // Check if the value is a property of the parentEntity
    var propertyOptional = filterEntityPropertyByName(parentEntity, propertyName);

    if (propertyOptional.isEmpty()) {
      throw new CodeGenerationException(
        "The entity Field named '" + baseField.getName() + "' declared in entity model '" + baseEntity.getName() + "'" +
          " contains the value '" + propertyName + "', which doesn't exists.");
    }

    var targetEntity = propertyOptional.get().getTargetEntity();
    if (!isLastValue && targetEntity == null) {
      throw new CodeGenerationException("Error in the value of the entity Field '" + baseField.getName() + "' declared in entity model '" + baseEntity.getName() + "'." +
        "The property '" + propertyOptional.get().getName() + "' does not contain a target entity.");
    }
    return targetEntity;
  }

  private static void manageFillTypes(List<Entity> entities, String packageEntities,
                                      ProjectionEntity entity, ProjectionEntityField field) throws CodeGenerationException {
    var entityModel = entities.stream()
      .filter(e -> e.getName().compareTo(entity.getName()) == 0)
      .findFirst();
    if (entityModel.isPresent()) {
      entity.setPackageName(entityModel.get().getPackageName());
      entity.setClassName(entityModel.get().getClassName());
      // TODO Fix identity kind through dictionary
      if (entity.getIdentity() != null && entity.getIdentity().compareTo("NONE") == 0) {
        entityModel.get().setHelp("identity=NONE");
      }
      if (field.getValue() != null && field.getName().compareTo(field.getValue()) == 0) {
        field.setValue(null);
      }

      var fieldModel = entityModel.get()
        .getProperties()
        .stream()
        .filter(p -> p.getName().compareTo(field.getName()) == 0)
        .findFirst();
      if (field.getValue() != null) {
        field.setClassName("String");
        if (!field.getValue().startsWith("#{")) {
          var value = field.getValue().split("\\.");
          if (value.length > 1) {
            StringBuilder getProperty = new StringBuilder();
            StringBuilder notNullProperty = new StringBuilder();
            var parentEntityModel = entityModel.get();
            for (var i = 0; i < value.length; i++) {
              parentEntityModel = validateParentEntityValue(entity, field, parentEntityModel, value[i], i == value.length - 1);
              getProperty.append(".get");
                  getProperty.append(value[i].substring(0, 1).toUpperCase());
                  getProperty.append(value[i].substring(1)).append("()");
              if (i < value.length - 1) {
                notNullProperty.append("#TARGET#").append(getProperty).append(" != null");
                if (i < value.length - 2) {
                  notNullProperty.append(" && ");
                }
              }
            }
            field.setValue(getProperty.toString());
            field.setNotNullValue(notNullProperty.toString());
            if (field.getType() == null) {
              field.setType("String");
            }
          }
        }

      } else {
        if (fieldModel.isPresent()) {
          field.setType(fieldModel.get().getTypeName());
          if (fieldModel.get().getTargetEntity() != null && fieldModel.get().getTargetEntity().getTableName() != null) {
            var m = fieldModel.get().getTargetEntity().getTableName().split("_");
            var cn = "";
            for (String s : m) {
              cn += s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
            }
            field.setClassName(cn);
          }
        } else {
          throw new CodeGenerationException(
            "The entity Field '" + field.getName() + "' declared in entity model '" + entity.getName() + "' doesn't exist");
        }
      }
    } else {
      // Try to find case typos
      var entityModelIgnoreCase = entities.stream()
        .filter(e -> e.getName()
          .compareToIgnoreCase(entity.getName()) == 0)
        .findFirst();
      if (entityModelIgnoreCase.isPresent()) {
        throw new CodeGenerationException(
          "Entitiy Model " + entity.getName() + " doesn't exist, maybe you refer to " +
            entityModelIgnoreCase.get().getName());
      } else {
        throw new CodeGenerationException("Entitiy Model " + entity.getName() + " doesn't exist");
      }
    }
    if (field.getType() == null) {
      throw new CodeGenerationException(
        "The entity Field '" + field.getName() + "' declared in entity model '" + entity.getName() + "' has null type");
    }
  }

  @FunctionalInterface
  public interface HandlingConsumer<T, E extends CodeGenerationException> {
    void accept(T target) throws E;

    static <T> Consumer<T> handlingConsumerBuilder(
      HandlingConsumer<T, CodeGenerationException> handlingConsumer) {
      return obj -> {
        try {
          handlingConsumer.accept(obj);
        } catch (CodeGenerationException ex) {
          throw new RuntimeException(ex);
        }
      };
    }
  }

}
