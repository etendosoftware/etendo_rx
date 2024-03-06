/*
 * Copyright 2022-2024  Futit Services SL
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
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This abstract class provides a base implementation for a JsonPathConverter.
 * It provides methods for reading JSON data using JsonPath and validating the read values.
 *
 * @param <E> The type of the object that will be returned after conversion.
 */
@Log4j2
public abstract class JsonPathConverterBase<E> implements JsonPathConverter<E> {

  /**
   * Reads a value from the provided DocumentContext at the specified path and returns it as an instance of the provided class.
   * If the value cannot be read or is not of the expected class, it returns a ReturnKey with error information.
   *
   * @param ctx   The DocumentContext from which to read the value.
   * @param path  The JsonPath at which to read the value.
   * @param clazz The class of the value to be read.
   * @return A ReturnKey containing the read value and error information.
   */
  public <F> ReturnKey<F> read(DocumentContext ctx, String path, Class<F> clazz) {
    try {
      F value = ctx.read(path, clazz);
      log.debug("    readedPath '{}' '{}'", path, value);
      // Is an error if the value is not of the expected class
      boolean nullValue = !clazz.isAssignableFrom(value.getClass());
      return new ReturnKey<>(path, value, false, nullValue, nullValue);
    } catch (Exception e) {
      log.debug("    Cannot read path '{}'", path);
      // Is not considered an error if the path is not found
      return new ReturnKey<>(path, null, true, true, false);
    }
  }

  @Override
  public DocumentContext getReadContext(String rawData) {
    Configuration conf = Configuration.defaultConfiguration().addOptions();
    return JsonPath.using(conf).parse(rawData);
  }

  /**
   * Validates the provided list of ReturnKey values.
   * If any ReturnKey has an error, it throws a ResponseStatusException with a message containing the paths of the erroneous ReturnKeys.
   * If any ReturnKey has a null value, it logs the paths of the ReturnKeys with null values.
   *
   * @param values The list of ReturnKey values to be validated.
   */
  protected void validateValues(List<ReturnKey<?>> values) {
    // If errors exists throw exception
    List<ReturnKey<?>> errors = values.stream().filter(ReturnKey::isError).toList();
    if (!errors.isEmpty()) {
      // Return a bad request exception with the errors as a comma separated string
      String errorString = errors.stream()
          .map(ReturnKey::getPath)
          .map(Object::toString)
          .collect(Collectors.joining(", "));
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
          "Error reading fields: " + errorString + " in conversion");
    }
    // Log in debug info missing fields
    List<ReturnKey<?>> missing = values.stream()
        .filter(e -> !e.isError() || e.isNullValue())
        .toList();
    if (!missing.isEmpty()) {
      log.debug("Missing fields: {}", missing.stream()
          .map(ReturnKey::getPath)
          .map(Object::toString)
          .collect(Collectors.joining(", ")));
    }
  }
}
