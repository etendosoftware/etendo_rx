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
package com.etendorx.das.utils;

import com.etendoerp.etendorx.data.ConstantValue;
import com.etendorx.entities.entities.BaseSerializableObject;
import com.etendorx.entities.entities.mappings.MappingUtils;
import com.etendorx.entities.jparepo.ETRX_Constant_ValueRepository;
import com.etendorx.utils.auth.key.context.AppContext;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.collection.spi.PersistentBag;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/**
 * This class provides utility methods for mapping objects. It is used to handle base objects, parse
 * date strings, and retrieve constant values.
 */
@Component
@Slf4j
public class MappingUtilsImpl implements MappingUtils {

  private final ETRX_Constant_ValueRepository constantValueRepository;

  public MappingUtilsImpl(ETRX_Constant_ValueRepository constantValueRepository) {
    this.constantValueRepository = constantValueRepository;
  }

  /**
   * Handle base object. If the object is a BaseSerializableObject, it will return the identifier
   * of the object. If the object is a PersistentBag, it will return a list of identifiers of the
   * objects in the bag. If the object is a Timestamp, it will return the formatted date according
   * to the user's date format and time zone.
   *
   * @param obj
   * @return the object
   */
  @Override
  public Object handleBaseObject(Object obj) {
    if (BaseSerializableObject.class.isAssignableFrom(obj.getClass())) {
      return handleBaseSerializableObject((BaseSerializableObject) obj);
    }
    if (PersistentBag.class.isAssignableFrom(obj.getClass())) {
      return handlePersistentBag((PersistentBag<?>) obj);
    }
    if (Date.class.isAssignableFrom(obj.getClass())) {
      String sdf = handleDateObject((Date) obj);
      if (sdf != null)
        return sdf;
    }
    return obj;
  }

  private static String handleBaseSerializableObject(BaseSerializableObject obj) {
    return obj.get_identifier();
  }

  @Nullable
  private static String handleDateObject(Date obj) {
    var dateTimeFormat = AppContext.getCurrentUser().getDateFormat();
    var timeZone = AppContext.getCurrentUser().getTimeZone();
    if (dateTimeFormat != null) {
      SimpleDateFormat sdf = new SimpleDateFormat(dateTimeFormat);
      if (timeZone != null) {
        sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
      }
      return sdf.format(obj);
    }
    return null;
  }

  private List<Object> handlePersistentBag(PersistentBag<?> obj) {
    List<Object> list = new ArrayList<>();
    for (Object o : (PersistentBag<?>) obj) {
      list.add(handleBaseObject(o));
    }
    return list;
  }

  /**
   * Parses a date string into a Date object according to the current user's date format and time zone.
   * If the date string is blank, it returns null.
   * If the current user's date format is null, it uses "yyyy-MM-dd" as the default date format.
   * If the date string is not blank and not equal to "null" (ignoring case), it tries to parse the date string.
   * If the parsing fails, it logs an error and returns null.
   *
   * @param date The date string to be parsed.
   * @return The parsed Date object, or null if the date string is blank or cannot be parsed.
   */
  @Override
  public Date parseDate(String date) {
    if (StringUtils.isBlank(date) || StringUtils.equalsIgnoreCase(date, "null")) {
      return null;
    }
    var dateTimeFormat = AppContext.getCurrentUser().getDateTimeFormat();
    if (dateTimeFormat == null) {
      dateTimeFormat = "yyyy-MM-dd HH:mm:ss";
    }
    var timeZone = AppContext.getCurrentUser().getTimeZone();
    Date returnValue = null;
    SimpleDateFormat sdf = new SimpleDateFormat(dateTimeFormat);
    if (timeZone != null) {
      sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
    }
    try {
      returnValue = sdf.parse(date);
    } catch (ParseException ignored) {
      // This error is ignored because the date may be in a different format
      // than the user's date format, and it will be parsed again below
    }
    if (returnValue != null) {
      return returnValue;
    }
    var dateFormat = AppContext.getCurrentUser().getDateFormat();
    if (dateFormat == null) {
      dateFormat = "yyyy-MM-dd";
    }
    sdf = new SimpleDateFormat(dateFormat);
    if (timeZone != null) {
      sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
    }
    try {
      return sdf.parse(date);
    } catch (ParseException e) {
      log.error("Error parsing date with value {}", date, e);
    }
    throw new IllegalArgumentException("The date " + date + " cannot be parsed");
  }

  /**
   * Retrieves the default value of a constant from the constant value repository by its identifier.
   * If the constant is not found, it returns null.
   *
   * @param id The identifier of the constant.
   * @return The default value of the constant, or null if the constant is not found.
   */
  @Override
  public String constantValue(String id) {
    var constantValue = constantValueRepository.findById(id);
    return constantValue.map(ConstantValue::getDefaultValue).orElse(null);
  }
}
