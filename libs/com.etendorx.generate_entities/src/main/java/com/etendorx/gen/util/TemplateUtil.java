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

import com.etendorx.gen.generation.GeneratePaths;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openbravo.base.model.Entity;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class TemplateUtil {
  private static final Logger log = LogManager.getLogger();

  /**
   * Process a template with the given data and write the result to the given output
   *
   * @param templateImplementation
   * @param data
   * @param output
   */
  public static void processTemplate(freemarker.template.Template templateImplementation,
      Map<String, Object> data, Writer output) {
    try {
      templateImplementation.process(data, output);
    } catch (IOException | TemplateException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Create a template implementation from the given file
   *
   * @param file
   */
  public static freemarker.template.Template createTemplateImplementation(String file) {
    log.debug("createTemplateImplementation: template {}", file);
    try (var stream = new BufferedReader(new InputStreamReader(
        Objects.requireNonNull(TemplateUtil.class.getResourceAsStream(file))))) {
      return new freemarker.template.Template("template", stream, getNewConfiguration());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  /**
   * Create a configuration for the template engine
   */
  private static Configuration getNewConfiguration() {
    final Configuration cfg = new Configuration();
    cfg.setObjectWrapper(new DefaultObjectWrapper());
    return cfg;
  }

  /**
   * Get the data needed for the template
   *
   * @param paths
   * @param entity
   * @param searchesMap
   * @param computedColumns
   * @param includeViews
   */
  public static Map<String, Object> getModelData(GeneratePaths paths, Entity entity,
      List<HashMap<String, Object>> searchesMap, boolean computedColumns, boolean includeViews) {
    final String newClassName = entity.getName();

    Map<String, Object> data = new HashMap<>();
    data.put("computedColumns", computedColumns);
    data.put("includeViews", includeViews);
    data.put("className", entity.getClassName().replaceAll("\\.", "/"));
    data.put("newClassName", newClassName);
    data.put("newClassNameWithoutS", getWithoutSS(newClassName));
    data.put("entity", entity);
    data.put("onlyClassName", data.get("className")
        .toString()
        .substring(data.get("className").toString().lastIndexOf('/') + 1));
    data.put("repositoryClassEntityModel", data.get("className")
        .toString()
        .replace(data.get("onlyClassName").toString(), entity.getName()) + "Model.java");
    data.put("tableName", entity.getTableName());
    data.put("packageEntities", paths.packageEntities);
    data.put("packageName", data.get("packageEntities").toString());
    data.put("searches", searchesMap);
    data.put("packageClientRest", "com.etendorx.clientrest");
    data.put("packageEntityModel",
        paths.pathEntitiesModelRx.substring(paths.pathEntitiesModelRx.lastIndexOf('/') + 1));
    return data;

  }

  /**
   * Remove the last s from the given string
   *
   * @param param
   */
  public static String getWithoutSS(String param) {
    if (param != null && param.length() > 0) {
      String lastCharacter = param.substring(Math.max(param.length() - 1, 0));
      String lastTwoCharacters = param.substring(Math.max(param.length() - 2, 0));
      if (lastTwoCharacters.equalsIgnoreCase("ss")) {
        return param.substring(0, param.length() - 1);
      }
      if (!lastCharacter.equalsIgnoreCase("s")) {
        return param.concat("s");
      }
    }
    return param;
  }

  /**
   * Prepare the output file
   *
   * @param fullPath
   * @param fileName
   */
  public static File prepareOutputFile(String fullPath, String fileName) {
    var outFile = new File(fullPath, fileName);
    new File(outFile.getParent()).mkdirs();
    return outFile;
  }

}
