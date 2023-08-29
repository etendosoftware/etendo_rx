package com.etendorx.entities.entities.mappings;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.collection.internal.PersistentBag;

import com.etendorx.entities.entities.BaseSerializableObject;

public class MappingUtils {
  private MappingUtils() {
  }

  public static Object handleBaseObject(Object obj) {
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
    return obj;
  }
}
