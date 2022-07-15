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

package com.etendorx.gen;

import com.etendorx.gen.process.GenerateProtoFile;
import com.etendorx.gen.util.CodeGenerationException;
import com.etendorx.gen.util.MetadataUtil;
import com.etendorx.gen.util.Projection;
import com.etendorx.gen.util.ProjectionEntity;
import com.etendorx.gen.util.TemplateUtil;
import com.etendorx.gen.util.Repository;
import freemarker.template.Template;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.etendorx.base.gen.Utilities;
import org.apache.logging.log4j.core.config.Configurator;
import org.etendorx.base.session.OBPropertiesProvider;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.ModelProvider;
import org.openbravo.base.model.Property;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.HashMap;

/**
 * Task generates the entities using the freemarker template engine.
 *
 * @author Sebastian Barrozo
 */
public class GenerateEntitiesApplication {
    private static final Logger log = LogManager.getLogger();
    public static final String ERROR_GENERATING_FILE = "Error generating file: ";
    public static final String GENERATING_FILE = "Generating file: ";
    public static final String PROJECTION_DEFAULT = "default";

    String FTL_FILE_NAME_CLIENTREST = "/org/openbravo/base/gen/clientRestProjectedRX.ftl";

    private String basePath;
    private String srcGenPath;
    private String propertiesFile;
    boolean generateAllChildProperties;
    boolean generateDeprecatedProperties;

    public final static String GENERATED_DIR = "/../build/tmp/generated";

    public static void main(String[] args) {
        // TODO: Allow the user to set the log level
        Configurator.setRootLevel(Level.INFO);
        new GenerateEntitiesApplication().run(args);
    }

    public void run(String... args) {

        final String srcPath = ".";
        String friendlyWarnings = "false";
        if (args.length >= 2) {
            friendlyWarnings = args[0];
        }
        final File baseDir = new File(srcPath);
        setPropertiesFile(baseDir.getAbsolutePath() + File.separator + "gradle.properties");
        execute(baseDir.getAbsolutePath());
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public boolean getFriendlyWarnings() {
        return OBPropertiesProvider.isFriendlyWarnings();
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

    public String getSrcGenPath() {
        return srcGenPath;
    }

    public void setSrcGenPath(String srcGenPath) {
        this.srcGenPath = srcGenPath;
    }

    public void execute(String baseDir) {
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

        final String pathEntitiesRx = pathEtendoRx + File.separator + "modules_gen" + File.separator + "com.etendorx.entities";
        final String pathJPARepoRx = pathEntitiesRx;
        final String pathClientRestRx = pathEtendoRx + File.separator + "modules_gen" + File.separator + "com.etendorx.entitiesModel";
        final String pathEntitiesModelRx = pathEtendoRx + File.separator + "modules_gen" + File.separator + "com.etendorx.entitiesModel";
        final String packageEntities = pathEntitiesRx.substring(pathEntitiesRx.lastIndexOf('/') + 1) + ".entities";
        final String baseRXObject = "BaseRXObject";

        final boolean generateRxCode = Boolean.parseBoolean(obProperties.getProperty("rx.generateCode"));
        final boolean computedColumns = Boolean.parseBoolean(obProperties.getProperty("rx.computedColumns"));
        if(computedColumns) {
            log.warn("*******************************************************************");
            log.warn("* DEPRECATED: Computed columns is not a recommended functionality *");
            log.warn("*******************************************************************");
        }
        final boolean includeViews = Boolean.parseBoolean(obProperties.getProperty("rx.views"));
        //
        final String dasUrl = obProperties.getProperty("dasUrl");
        log.info("Generate Etendo Rx Code={}", generateRxCode);
        log.info("Path Project Rx={}", pathEtendoRx);
        log.info("Path Entities Rx={}", pathEntitiesRx);
        log.info("Path JPA Repo Rx={}", pathJPARepoRx);
        log.info("DAS Url={}", dasUrl);

        var metadata = MetadataUtil.analizeMetadata(pathEtendoRx);

        generateAllChildProperties = OBPropertiesProvider.getInstance()
            .getBooleanProperty("hb.generate.all.parent.child.properties");

        generateDeprecatedProperties = OBPropertiesProvider.getInstance()
            .getBooleanProperty("hb.generate.deprecated.properties");

        // process template & write file for each entity
        List<Entity> entities = ModelProvider.getInstance().getModel();
        ModelProvider.getInstance().addHelpAndDeprecationToModel(generateDeprecatedProperties);

        try {
            metadata = MetadataUtil.fillTypes(metadata, entities, packageEntities);
            boolean hasErrors = false;
            log.info("****************************");
            log.info(" Checking entities name in metadata.json ");
            for (Projection value : metadata.getProjections().values()) {
                for (var entity : value.getEntities().values()) {
                    if (entity.getPackageName() == null) {
                        log.error("  - Cannot find entity '{}'", entity.getName());
                        hasErrors = true;
                    }
                }
            }
            if (hasErrors) {
                throw new CodeGenerationException("Check entities name");
            } else {
                log.info(" - All entities ok ");
            }
        } catch (CodeGenerationException e) {
            log.error("Error validating metadata file {}", e.getMessage());
            throw new RuntimeException(e);
        } finally {
            log.info("****************************");
        }
        var projections = new ArrayList<Projection>();
        var projectionDefault = new Projection(PROJECTION_DEFAULT);
        projections.add(projectionDefault);
        projections.addAll(metadata.getProjections().values());

        try {
            for (Entity entity : entities) {
                // If the entity is associated with a datasource based table or based on an HQL query, do not
                // generate a Java file
                if (entity.isDataSourceBased() || entity.isHQLBased()) {
                    continue;
                }

                if (generateRxCode && !entity.isVirtualEntity() && (includeViews || !entity.isView())) {

                    var data = TemplateUtil.getModelData(entity, computedColumns, includeViews);
                    data.put("packageEntities", packageEntities);
                    data.put("packageName", data.get("packageEntities").toString());
                    data.put("dasUrl", dasUrl);
                    if (metadata.getRepositories().containsKey(data.get("newClassName").toString())) {
                        data.put("searches",
                            metadata.getRepositories().get(data.get("newClassName").toString()).getSearchesMap());
                    }

                    generateEntityRX(data, pathEntitiesRx);

                    generateJPARepo(data, pathJPARepoRx);

                    data.put("packageClientRest", "com.etendorx.clientrest");
                    data.put("packageEntityModel",
                        pathEntitiesModelRx.substring(pathEntitiesModelRx.lastIndexOf('/') + 1));

                    generateClientRestRX(data, pathEtendoRx);

                    generateEntityModel(data, pathEtendoRx);

                    for (Projection projection : projections) {
                        if (projection.getEntities().size() == 0 || projection.getEntities().containsKey(
                            data.get("newClassName").toString())) {

                            generateProjections(data, pathJPARepoRx, projection, entity);

                            generateModelProjected(data, pathEntitiesModelRx, projection, entity);
                        }
                    }
                }
            }

            // TODO: Loop over all the 'metadata' objects and create the:
            // 'projections' (used by the JPA) defined by the user
            // 'model projected' (used by the feign client to store the data)
            // 'feign clients' (to make the http request to the DAS) should contain the 'searches' query's
            // Each metadata object should be created from each module.

            generateBaseEntityRx(new HashMap<>(), pathEntitiesRx, baseRXObject, packageEntities);

            // Generate Proto File
            new GenerateProtoFile().generate(pathEtendoRx, metadata.getRepositoriesMap(), projections, computedColumns, includeViews);
        } catch (IOException e) {
            log.error(ERROR_GENERATING_FILE + GENERATED_DIR, e);
        }


        log.info("Generated " + entities.size() + " entities");
    }



    private void generateEntityModel(Map<String, Object> data, String pathClientRestRx) throws FileNotFoundException {

        String ftlFileNameEntitiesModel = "/org/openbravo/base/gen/entityModel.ftl";
        Template templateEntityModelRX = TemplateUtil.createTemplateImplementation(
            ftlFileNameEntitiesModel);

        final String packageEntityModel = "com.etendorx.entitiesmodel";
        data.put("packageEntityModel", packageEntityModel);
        final String fullPathClientRest = pathClientRestRx + "/modules_gen/com.etendorx.entitiesModel" + "/src/main/java/" + packageEntityModel.toLowerCase().replace(
            '.', '/');
        final String repositoryClassEntityModel = data.get("repositoryClassEntityModel").toString();

        var outFileEntityModel = new File(fullPathClientRest, repositoryClassEntityModel);
        new File(outFileEntityModel.getParent()).mkdirs();

        Writer outWriterEntityModel = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(outFileEntityModel), StandardCharsets.UTF_8));
        TemplateUtil.processTemplate(templateEntityModelRX, data, outWriterEntityModel);

    }

    private void generateClientRestRX(Map<String, Object> data, String pathEtendoRx) throws FileNotFoundException {
        String pathClientRestRx = pathEtendoRx + File.separator + "modules_gen" + File.separator + "com.etendorx.entitiesModel";
        String ftlFileNameClientRest = "/org/openbravo/base/gen/clientRestRX.ftl";
        freemarker.template.Template templateClientRestRX = TemplateUtil.createTemplateImplementation(
            ftlFileNameClientRest);

        final String packageClientRest = data.get("packageClientRest").toString();
        final String fullPathClientRest = pathClientRestRx + "/src/main/java/" +
            packageClientRest.toLowerCase().replace('.', '/') + "/" +
            ((Entity) data.get("entity")).getPackageName().replace('.', '/');
        final String repositoryClassClientRest = data.get("newClassName") + "ClientRest.java";
        var outFileClientRest = new File(fullPathClientRest, repositoryClassClientRest);
        new File(outFileClientRest.getParent()).mkdirs();

        Writer outWriterClientRest = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(outFileClientRest), StandardCharsets.UTF_8));
        data.put("packageClientRest", packageClientRest);
        TemplateUtil.processTemplate(templateClientRestRX, data, outWriterClientRest);
    }

    private void generateJPARepo(Map<String, Object> data, String pathJPARepoRx) throws FileNotFoundException {
        String ftlFileNameJPARepo = "/org/openbravo/base/gen/jpaRepoRX.ftl";
        freemarker.template.Template templateJPARepoRX = TemplateUtil.createTemplateImplementation(
            ftlFileNameJPARepo);

        final String packageJPARepo = pathJPARepoRx.substring(pathJPARepoRx.lastIndexOf('/') + 1) + ".jparepo";
        final String fullPathJPARepo = pathJPARepoRx + "/src/main/jparepo/" + packageJPARepo.replace(
            '.', '/');
        final String repositoryClass = org.etendorx.base.gen.Utilities.toCamelCase(
            data.get("tableName").toString()) + "Repository.java";
        new File(fullPathJPARepo).mkdirs();
        var outFileRepo = new File(fullPathJPARepo, repositoryClass);

        data.put("packageJPARepo", packageJPARepo);
        Writer outWriterRepo = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFileRepo), StandardCharsets.UTF_8));
        TemplateUtil.processTemplate(templateJPARepoRX, data, outWriterRepo);
    }

    private void generateBaseEntityRx(Map<String, Object> data, String pathEntitiesRx, String className, String packageName) throws FileNotFoundException {
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

    private void generateEntityRX(Map<String, Object> data, String pathEntitiesRx) throws FileNotFoundException {
        final String className = data.get("className").toString();
        final String onlyClassName = data.get("onlyClassName").toString();
        final String packageEntities = data.get("packageEntities").toString();
        final String fullPathEntities = pathEntitiesRx + "/src/main/entities/" + packageEntities.replace(
            '.', '/');
        var classfileName = className.replace(onlyClassName,
            org.etendorx.base.gen.Utilities.toCamelCase(
                data.get("tableName").toString())) + ".java";
        log.debug(GENERATING_FILE + classfileName);
        var outFile = new File(fullPathEntities, classfileName);
        new File(outFile.getParent()).mkdirs();
        String ftlFileNameRX = "/org/openbravo/base/gen/entityRX.ftl";
        freemarker.template.Template templateRX = TemplateUtil.createTemplateImplementation(
            ftlFileNameRX);
        Writer outWriter = new BufferedWriter(
            new OutputStreamWriter(new FileOutputStream(outFile), StandardCharsets.UTF_8));
        TemplateUtil.processTemplate(templateRX, data, outWriter);
    }

    private String getTemplatesDir() {
        return "";
    }

    private void generateProjections(
            Map<String, Object> data, String pathJPARepoRx, Projection projection, Entity entity) throws FileNotFoundException {

        String ftlFileNameProjectionRepo = "/org/openbravo/base/gen/jpaProjectionRX.ftl";
        Template templateJPAProjectionRX = TemplateUtil.createTemplateImplementation(
            ftlFileNameProjectionRepo);

        String projectionName = projection.getName();
        ProjectionEntity projectionEntity = projection.getEntities().getOrDefault(data.get("newClassName").toString(), null);

        final String className = data.get("className").toString();
        final String onlyClassName = data.get("onlyClassName").toString();
        final String packageEntities = data.get("packageEntities").toString();
        final String packageProjectionRepo = pathJPARepoRx.substring(pathJPARepoRx.lastIndexOf('/') + 1);
        final String fullPathProjectionRepo = pathJPARepoRx + "/src/main/projections/" + packageEntities.replace(
            '.', '/');
        final String projectionClass = className.replace(onlyClassName,
            org.etendorx.base.gen.Utilities.toCamelCase(
                entity.getTableName())) + StringUtils.capitalize(
            projectionName) + "Projection.java";

        var outFileProjection = new File(fullPathProjectionRepo, projectionClass);
        new File(outFileProjection.getParent()).mkdirs();

        Writer outWriterProjection = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFileProjection), StandardCharsets.UTF_8));
        data.put("projectionName", projectionName);
        data.put("projectionFields",
            projectionEntity != null ? projectionEntity.getFieldsMap() : new ArrayList<String>());
        TemplateUtil.processTemplate(templateJPAProjectionRX, data, outWriterProjection);
    }

    /**
     * Generates the java class corresponding with the entity used in a projection.
     * The purpose of the generated object is to store the 'data' fetched using the feign client when making a request.
     * @param data
     * @param pathEntitiesModelRx
     * @param projection
     * @param entity
     * @throws FileNotFoundException
     */
    private void generateModelProjected(
            Map<String, Object> data, String pathEntitiesModelRx, Projection projection, Entity entity) throws FileNotFoundException {

        String ftlFileNameProjectionRepo = "/org/openbravo/base/gen/entityModelProjected.ftl";
        Template templateModelProjectionRX = TemplateUtil.createTemplateImplementation(
            ftlFileNameProjectionRepo);

        final var projectionName = projection.getName();
        ProjectionEntity projectionEntity = projection.getEntities().getOrDefault(data.get("newClassName").toString(), null);

        final String className = data.get("className").toString();
        final String onlyClassName = data.get("onlyClassName").toString();
        final String packageEntities = data.get("packageEntities").toString();

        String fullPathProjectionRepo = pathEntitiesModelRx + "/src/main/java/" + packageEntities.replace('.', '/');
        final String projectionClass = className.replace(onlyClassName,
                org.etendorx.base.gen.Utilities.toCamelCase(
                        entity.getTableName())) + StringUtils.capitalize(projectionName) + "Model.java";

        String packageEntityModelProjected = packageEntities + "." + entity.getPackageName();

        if (!StringUtils.equals(PROJECTION_DEFAULT, projection.getName())) {
            fullPathProjectionRepo = MetadataUtil.getBasePackageGenLocationPath(projection.getModuleLocation()) + File.separator + MetadataUtil.ENTITY_PACKAGE;
            packageEntityModelProjected = projection.getModuleLocation().getName() + "." + MetadataUtil.ENTITY_PACKAGE + "." + entity.getPackageName();
        }

        // Set the package of the projected model
        data.put("packageEntityModelProjected", packageEntityModelProjected);

        var outFileProjection = new File(fullPathProjectionRepo, projectionClass);
        new File(outFileProjection.getParent()).mkdirs();

        Writer outWriterProjection = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFileProjection), StandardCharsets.UTF_8));
        data.put("projectionName", projectionName);
        data.put("projectionFields",
            projectionEntity != null ? projectionEntity.getFieldsMap() : new ArrayList<String>());
        TemplateUtil.processTemplate(templateModelProjectionRX, data, outWriterProjection);
    }

    /**
     * Generates the feign client java class corresponding with the defined entity to be projected.
     * - TODO enhancement: Create a 'feignClientTemplate' to store all the dynamic values used in the 'ftl' file.
     * - Make use of 'jackson' to parse the java class to a HashMap
     *
     * @param data
     * @param pathEntitiesModelRx
     * @param projection
     * @param entity
     * @param repository
     * @throws FileNotFoundException
     */
    private void generateClientRestProjected(
            Map<String, Object> data, String pathEntitiesModelRx, Projection projection, Entity entity, Repository repository
    ) throws FileNotFoundException {

        // TODO: Obtain the 'searches' from the repository to create the custom user query's.
        // The repository 'entityName' should match with the 'Entity' name.
        // The 'repository' should belong to the same 'projection' module
        // If the repository 'entityName' refers to an entity that is not defined in the 'projection' entities, then should use the 'default' one.
        // This is the case where the 'metadata' only contains repositories searches, and a feign client should be created.
        // In other case, use the defined 'user' projection entity. (The feign client will extend from the entity defined by the user or a default one.)

        freemarker.template.Template templateClientRestRX = TemplateUtil.createTemplateImplementation(
                FTL_FILE_NAME_CLIENTREST);

        String fullPathClientRestGen = MetadataUtil.getBasePackageGenLocationPath(projection.getModuleLocation()) + File.separator + MetadataUtil.CLIENTREST_PACKAGE;
        final String clientRestClassNameGen = data.get("newClassName") + "ClientRest.java";

        // Set the parametrized class from where the client rest will extend (ClientRestBase<'classname'>)
        final String newClassName = data.get("newClassName").toString();

        /**
         * Contains the complete name of the created projection model (package + class name + projection name + 'Model')
         * The package and class name are obtained from the {@link Entity}
         */
        final String modelProjectionClass = ((Entity) data.get("entity")).getPackageName() +
                "." +  newClassName + StringUtils.capitalize(projection.getName()) + "Model";

        // Contains the complete package and model class name
        final String locationModelProjectionClass = projection.getModuleLocation().getName() + "." + MetadataUtil.ENTITY_PACKAGE + "." + modelProjectionClass;
        data.put("locationModelProjectionClass", locationModelProjectionClass);

        // TODO: create the name using the module db prefix
        // Generate the feignClientName used by the @FeignClient annotation.
        String moduleName = projection.getModuleLocation().getName();
        String feignClientName = data.get("newClassName") + "-" + moduleName;
        data.put("feignClientName", feignClientName);

        var outFileClientRest = new File(fullPathClientRestGen, clientRestClassNameGen);
        new File(outFileClientRest.getParent()).mkdirs();

        // Set the package of the autogenerated client rest class
        String packageClientRestProjected = projection.getModuleLocation().getName() + "." + MetadataUtil.CLIENTREST_PACKAGE;
        data.put("packageClientRestProjected", packageClientRestProjected);

        Writer outWriterClientRest = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(outFileClientRest), StandardCharsets.UTF_8));
        TemplateUtil.processTemplate(templateClientRestRX, data, outWriterClientRest);
    }

    private void addClassInGenerated(FileOutputStream excludedFilter, Entity entity, String suffix) {
        if (excludedFilter != null) {
            try {
                if (suffix != null) {
                    excludedFilter.write(entity.getClassName().concat(suffix).getBytes());
                } else {
                    excludedFilter.write(entity.getClassName().getBytes());
                }
                excludedFilter.write("\n".getBytes());

            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    /**
     * Checks if "hb.generate.deprecated.properties" or "hb.generate.all.parent.child.properties"
     * properties from Openbravo.properties have been set to true. If so, then deprecation should be
     * added.
     *
     * @return True if deprecation should be added, depending on global properties found in
     *     Openbravo.properties, else false.
     */
    public boolean shouldAddDeprecation() {
        return generateDeprecatedProperties || generateAllChildProperties;
    }

    /**
     * Checks if an entity is set as deprecated
     *
     * @param e
     *     Entity to check deprecation
     *
     * @return True if entity is deprecated, false otherwise
     */
    public boolean isDeprecated(Entity e) {
        return e.isDeprecated() != null && e.isDeprecated();
    }

    /**
     * Checks if a proprerty is deprecated, it can be deprecated in Application Dictionary or the
     * entity it references could be deprecated
     *
     * @param p
     *     Property to check deprecation
     *
     * @return True if property or property target entity are deprecated and generate deprecate
     *     property is set to true in Openbravo.properties, false otherwise
     */
    public boolean isDeprecated(Property p) {
        if ((p.isDeprecated() != null && p.isDeprecated()) || (p.getTargetEntity() != null
                && p.getTargetEntity().isDeprecated() != null && p.getTargetEntity().isDeprecated())) {
            return true;
        }

        Property refPropery = p.getReferencedProperty();
        if (refPropery == null) {
            return false;
        }

        boolean generatedInAnyCase = ModelProvider.getInstance()
                .shouldGenerateChildPropertyInParent(refPropery, false);

        boolean generatedDueToPreference = ModelProvider.getInstance()
                .shouldGenerateChildPropertyInParent(refPropery, true);
        return !generatedInAnyCase && generatedDueToPreference;
    }

    public String getDeprecationMessage(Property p) {
        if (p.isDeprecated() != null && p.isDeprecated()) {
            return "Property marked as deprecated on field Development Status";
        }
        if (p.getTargetEntity() != null && p.getTargetEntity().isDeprecated() != null
                && p.getTargetEntity().isDeprecated()) {
            return "Target entity {@link " + p.getTargetEntity().getSimpleClassName()
                    + "} is deprecated.";
        }
        return "Child property in parent entity generated for backward compatibility, it will be removed in future releases.";
    }

    private boolean hasChanged() {
        // first check if there is a directory
        // already in the src-gen
        // if not then regenerate anyhow
        final File modelDir = new File(getSrcGenPath(),
                "org" + File.separator + "openbravo" + File.separator + "model" + File.separator + "ad");
        if (!modelDir.exists()) {
            return true;
        }

        // check if the logic to generate has changed...
        final String sourceDir = getBasePath();
        long lastModifiedPackage = 0;
        lastModifiedPackage = getLastModifiedPackage("org.openbravo.base.model", sourceDir,
                lastModifiedPackage);
        lastModifiedPackage = getLastModifiedPackage("org.openbravo.base.gen", sourceDir,
                lastModifiedPackage);
        lastModifiedPackage = getLastModifiedPackage("org.openbravo.base.structure", sourceDir,
                lastModifiedPackage);

        // check if there is a sourcefile which was updated before the last
        // time the model was created. In this case that sourcefile (and
        // all source files need to be regenerated
        final long lastModelUpdateTime = ModelProvider.getInstance().computeLastUpdateModelTime();
        final long lastModified;
        if (lastModelUpdateTime > lastModifiedPackage) {
            lastModified = lastModelUpdateTime;
        } else {
            lastModified = lastModifiedPackage;
        }
        return isSourceFileUpdatedBeforeModelChange(modelDir, lastModified);
    }

    private boolean isSourceFileUpdatedBeforeModelChange(File file, long modelUpdateTime) {
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                if (isSourceFileUpdatedBeforeModelChange(child, modelUpdateTime)) {
                    return true;
                }
            }
            return false;
        }
        return file.lastModified() < modelUpdateTime;
    }

    private long getLastModifiedPackage(String pkg, String baseSourcePath, long prevLastModified) {
        final File file = new File(baseSourcePath, pkg.replaceAll("\\.", "/"));
        final long lastModified = getLastModifiedRecursive(file);
        if (lastModified > prevLastModified) {
            return lastModified;
        }
        return prevLastModified;
    }

    private long getLastModifiedRecursive(File file) {
        long lastModified = file.lastModified();
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                final long childLastModified = getLastModifiedRecursive(child);
                if (lastModified < childLastModified) {
                    lastModified = childLastModified;
                }
            }
        }
        return lastModified;
    }

    public String formatSqlLogic(String sqlLogic) {
        if (sqlLogic != null) {
            final String sqlLogicEscaped = sqlLogic.replaceAll("\\*/", " ");
            final String wrappedSqlLogic = WordUtils.wrap(sqlLogicEscaped, 100);
            return wrappedSqlLogic.replaceAll("\n", "\n       ");
        } else {
            return sqlLogic;
        }
    }

}
