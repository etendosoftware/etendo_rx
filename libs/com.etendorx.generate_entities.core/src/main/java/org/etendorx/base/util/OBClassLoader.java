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

package org.etendorx.base.util;

import org.etendorx.base.provider.OBProvider;
import org.etendorx.base.provider.OBSingleton;

/**
 * The OBClassLoader is used to support different classloading scenarios. As a default two
 * classloaders are present: the context (the default, used in Tomcat) and the class classloader.
 * The class classloader is used in Ant tasks.
 * <p>
 * Use the {@link OBProvider OBProvider} to define which classloader in a specific environment.
 *
 * @author mtaal
 */

public class OBClassLoader implements OBSingleton {

  private static OBClassLoader instance;

  public static OBClassLoader getInstance() {
    if (instance == null) {
      instance = OBProvider.getInstance().get(OBClassLoader.class);
    }
    return instance;
  }

  /**
   * Load a class using the classloader. This method will throw an OBException if the class is not
   * found. This exception is logged.
   *
   * @param className
   *     the name of the class to load
   */
  public Class<?> loadClass(String className) throws ClassNotFoundException {
    return Thread.currentThread().getContextClassLoader().loadClass(className);
  }

  /**
   * A class loader which uses the classloader of the Class class.
   * <p>
   * To use this classloader do the following:
   * OBProvider.getInstance().register(OBClassLoader.class, OBClassLoader.ClassOBClassLoader.class,
   * false);
   */
  public static class ClassOBClassLoader extends OBClassLoader {

    @Override
    public Class<?> loadClass(String className) throws ClassNotFoundException {
      return Class.forName(className);
    }
  }
}
