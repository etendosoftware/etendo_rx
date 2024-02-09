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
package com.etendorx.entities.mapper.lib;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import lombok.extern.log4j.Log4j2;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Log4j2
public abstract class JsonPathConverterBase<E> implements JsonPathConverter<E> {
  public class ReturnKey<E> {
    public E value;
    public boolean missing;

    public ReturnKey(E value, boolean missing) {
      this.value = value;
      this.missing = missing;
    }
  }

  protected <E> ReturnKey<E> read(DocumentContext ctx, String path, Class<E> clazz) {
    try {
      E value = ctx.read(path, clazz);
      log.debug("    readedPath '{}' '{}'", path, value);
      return new ReturnKey<>(value, false);
    } catch (Exception e) {
      log.debug("    Cannot read path '{}'", path);
      return new ReturnKey<>(null, true);
    }
  }

  @Override
  public DocumentContext getReadContext(String rawData) {
    Configuration conf = Configuration.defaultConfiguration()
        .addOptions();
    return JsonPath.using(conf).parse(rawData);
  }

  public <E> Optional<E> getData(DocumentContext ctx, String path, Class<E> type, Function<String, Void> missing) {
    var language = read(ctx, path, type);
    if(language.missing) {
      missing.apply(path);
      return Optional.empty();
    } else {
      return Optional.ofNullable(language.value);
    }
  }

  public Optional<Object> getData(DocumentContext ctx, String path, Function<String, Void> missing) {
    return getData(ctx, path, Object.class, missing);
  }
  protected String debugFields(List<String> expected, Set<String> received, List<String> missing) {
    List<String> unexpectedFields = received.stream()
        .filter(s -> !expected.contains("$." + s))
        .toList();
    String debugString = "MISSING FIELDS: [\n";
    debugString += missing.stream().reduce("", (acc, field) -> acc + " " + field + "\n");
    debugString += "]\n";
    debugString += "UNEXPECTED FIELDS: [\n";
    debugString += unexpectedFields.stream().reduce("", (acc, field) -> acc + " $." + field + "\n");
    debugString += "]\n";
    return debugString;
  }
}
