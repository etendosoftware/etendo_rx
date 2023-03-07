package com.etendorx.entities.utilities;

import java.util.HashMap;

public class KeyValueMap extends HashMap<String, Object> {

  public KeyValueMap(Object... values) {
    for (int i = 0; i < values.length; i += 2) {
      put((String) values[i], values[i + 1]);
    }
  }

}
