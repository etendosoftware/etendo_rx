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
package com.etendorx.das.utils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.hibernate.collection.internal.PersistentBag;
import org.springframework.stereotype.Component;

import com.etendorx.entities.entities.BaseSerializableObject;
import com.etendorx.entities.entities.mappings.MappingUtils;
import com.etendorx.utils.auth.key.context.AppContext;

/**
 * Mapping utils implementation for the DAS module
 */
@Component
public class MappingUtilsImpl implements MappingUtils {

  /**
   * Handle base object. If the object is a BaseSerializableObject, it will return the identifier
   * of the object. If the object is a PersistentBag, it will return a list of identifiers of the
   * objects in the bag. If the object is a Timestamp, it will return the formatted date according
   * to the user's date format and time zone.
   * @param obj
   * @return the object
   */
  public Object handleBaseObject(Object obj) {
    if(BaseSerializableObject.class.isAssignableFrom(obj.getClass())) {
      return ((BaseSerializableObject) obj).get_identifier();
    }
    if(PersistentBag.class.isAssignableFrom(obj.getClass())) {
      List<Object> list = new ArrayList<>();
      for(Object o : (PersistentBag) obj) {
        list.add(handleBaseObject(o));
      }
      return list;
    }
    if(Timestamp.class.isAssignableFrom(obj.getClass())) {
      var dateFormat = AppContext.getCurrentUser().getDateFormat();
      var timeZone = AppContext.getCurrentUser().getTimeZone();
      if(dateFormat != null) {
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        if(timeZone != null) {
          sdf.setTimeZone(TimeZone.getTimeZone(timeZone));
        }
        return sdf.format((Timestamp) obj);
      }
    }
    return obj;
  }
}