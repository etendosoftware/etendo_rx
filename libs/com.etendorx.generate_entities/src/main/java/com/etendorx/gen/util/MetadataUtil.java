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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.etendorx.base.exception.OBException;
import org.etendorx.base.gen.Utilities;
import org.openbravo.base.model.Entity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Util class needed for code generation
 *
 * @author Sebastian Barrozo
 */

public class MetadataUtil {
  private static final Logger log = LogManager.getLogger();
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

  /**
   * Name of the packages used to store the autogenerated classes in each module
   */
  public static final String ENTITY_PACKAGE = "entities";
  public static final String CLIENTREST_PACKAGE = "clientrest";


  // TODO: Each module should contain his own 'metadata' object
  public static Metadata analizeMetadata(String pathEtendoRx) throws OBException {
    var metadata = new Metadata();
    log.info("Search projections in path {}/{}",
        pathEtendoRx, "modules");
    var directories = getMetadataFiles(pathEtendoRx + File.separator + "modules" + File.separator);
    directories.addAll(getMetadataFiles(pathEtendoRx + File.separator + "modules_core" + File.separator));
    for (File dir : directories) {
      var metadataPath = dir + File.separator + "src-db" + File.separator + "das" + File.separator + "metadata.json";
      var projectionsFile = new File(metadataPath);
      if (projectionsFile.exists()) {
        log.info("Founded metadata {}", metadataPath);
        try {
          analizeProjections(metadata.getProjections(), projectionsFile, dir);
          analizeRepositories(metadata.getRepositories(), projectionsFile);
        } catch (JSONException | IOException | CodeGenerationException exception) {
          exception.printStackTrace();
          throw new OBException(exception.getMessage());
        }
      }
    }
    return metadata;
  }

  /**
   * Obtains the absolute path of the base package where all the java files will be created.
   * Ex: given the module 'com.test.mymodule'
   *    returns 'module.absolutePath + /src-gen/main/java/com/test/mymodule'
   * @param moduleLocation
   * @return
   */
  public static String getBasePackageGenLocationPath(File moduleLocation) {
    return getSrcGenLocation(moduleLocation) + File.separator + "/main/java" + File.separator + getBasePackageGenPath(moduleLocation);
  }

  /**
   * Obtains the absolute path where all generated files will be stored
   * @param moduleLocation
   * @return
   */
  public static String getSrcGenLocation(File moduleLocation) {
    return moduleLocation.getAbsolutePath() + File.separator + "src-gen";
  }

  /**
   * Parses the name of the module to a package path.
   * Ex: 'com.etendorx.mymodule' -> 'com/etendorx/mymodule'
   * @param moduleLocation
   * @return
   */
  public static String getBasePackageGenPath(File moduleLocation) {
    return moduleLocation.getName().replace("\\.", "/");
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

  private static void analizeProjections(Map<String, Projection> projections, File projectionsFile, File moduleLocation)
      throws IOException, JSONException, CodeGenerationException {
    var content = Files.readString(projectionsFile.toPath());
    var jsonProps = new JSONObject(content);
    if (jsonProps.has(PROJECTIONS)) {
      var projs = jsonProps.getJSONArray(PROJECTIONS);
      for (var i = 0; i < projs.length(); i++) {
        var jsonProjection = projs.getJSONObject(i);
        var name = jsonProjection.getString(NAME);
        boolean grpc = false;
        if (jsonProjection.has(GRPC)) {
          grpc = jsonProjection.getBoolean(GRPC);
        }
        // TODO: If different modules use the same name of a projection, then the location module will be incorrect.
        Projection projection = getProjection(projections, name, grpc);
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
                                          File projectionsFile) throws IOException, JSONException {
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
            repositorySearch = getRepositorySearch(repository, method, query, fetchAttributes);
            if (jsonSearch.has(PARAMS)) {
              var params = jsonSearch.getJSONArray(PARAMS);
              log.info("Analyzing params {}", params);
              for (var k = 0; k < params.length(); k++) {
                var field = params.getJSONObject(k);
                if (field.has(NAME)) {
                  var paramName = field.getString(NAME);
                  var paramType = field.getString(TYPE);
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

  private static Projection getProjection(Map<String, Projection> projections, String projectionName, boolean grpc) {
    Projection projection;
    if (projections.containsKey(projectionName)) {
      projection = projections.get(projectionName);
    } else {
      projection = new Projection(projectionName, grpc);
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

  private static void manageFillTypes(List<Entity> entities, String packageEntities,
                                      ProjectionEntity entity, ProjectionEntityField field) throws CodeGenerationException {
    var entityModel = entities.stream()
        .filter(e -> Utilities.toCamelCase(e.getTableName()).compareTo(entity.getName()) == 0)
        .findFirst();
    if (entityModel.isPresent()) {
      entity.setPackageName(packageEntities + "." + entityModel.get().getPackageName());
      // TODO Fix identity kind through dictionary
      if (entity.getIdentity() != null && entity.getIdentity().compareTo("NONE") == 0) {
        entityModel.get().setHelp("identity=NONE");
      }
      var fieldModel = entityModel.get()
          .getProperties()
          .stream()
          .filter(p -> p.getName().compareTo(field.getName()) == 0)
          .findFirst();
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
    } else {
      // Try to find case typos
      var entityModelIgnoreCase = entities.stream()
          .filter(e -> Utilities.toCamelCase(e.getTableName())
              .compareToIgnoreCase(entity.getName()) == 0)
          .findFirst();
      if (entityModelIgnoreCase.isPresent()) {
        throw new CodeGenerationException(
            "Entitiy Model " + entity.getName() + " doesn't exist, maybe you refer to " + Utilities.toCamelCase(
                entityModelIgnoreCase.get().getTableName()));
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
