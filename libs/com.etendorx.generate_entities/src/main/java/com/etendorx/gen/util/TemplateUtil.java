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

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.TemplateException;
import org.etendorx.base.gen.Utilities;
import org.openbravo.base.model.Entity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class TemplateUtil {
  public static void processTemplate(freemarker.template.Template templateImplementation,
      Map<String, Object> data, Writer output) {
    try {
      templateImplementation.process(data, output);
    } catch (IOException | TemplateException e) {
      throw new IllegalStateException(e);
    }
  }

  public static freemarker.template.Template createTemplateImplementation(String file) {
    try (var stream = new BufferedReader(new InputStreamReader(
        Objects.requireNonNull(TemplateUtil.class.getResourceAsStream(file))))) {
      return new freemarker.template.Template("template", stream, getNewConfiguration());
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  private static Configuration getNewConfiguration() {
    final Configuration cfg = new Configuration();
    cfg.setObjectWrapper(new DefaultObjectWrapper());
    return cfg;
  }

  public static Map<String, Object> getModelData(Entity entity, boolean computedColumns, boolean includeViews) {
    final String newClassName = entity.getName();

    Map<String, Object> data = new HashMap<>();
    // TODO: Create constant and add better descriptions
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
        .replace(data.get("onlyClassName").toString(),
            entity.getName()) + "Model.java");
    //data.put("util", this);
    data.put("tableName", entity.getTableName());

    return data;

  }

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
}
