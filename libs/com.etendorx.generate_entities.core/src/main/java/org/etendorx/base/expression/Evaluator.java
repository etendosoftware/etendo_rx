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

package org.etendorx.base.expression;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.etendorx.base.exception.OBException;
import org.etendorx.base.provider.OBProvider;
import org.etendorx.base.provider.OBSingleton;
import org.etendorx.base.util.Check;
import org.openbravo.base.model.BaseOBObjectDef;
import org.openbravo.base.model.Entity;
import org.openbravo.base.model.Property;

import javax.script.ScriptException;

import java.util.HashMap;
import java.util.Map;

/**
 * Evaluates expressions in the context of a business object, the expression language supported by
 * this class is javascript rhino.
 *
 * @author mtaal
 */

public class Evaluator implements OBSingleton {
  private static final Logger log = LogManager.getLogger();

  private static Evaluator instance = new Evaluator();

  public static synchronized Evaluator getInstance() {
    if (instance == null) {
      instance = OBProvider.getInstance().get(Evaluator.class);
    }
    return instance;
  }

  public static synchronized void setInstance(Evaluator instance) {
    Evaluator.instance = instance;
  }

  /**
   * Evaluates the passed script in the context of the passed business object. This means that
   * properties of the business object may be used directly in the script. The result should always
   * be a boolean.
   *
   * @param contextBob
   *     the script is executed in the context of this business object
   * @param script
   *     the javascript which much evaluate to a boolean
   * @return the result of the script evaluation
   */
  public Boolean evaluateBoolean(BaseOBObjectDef contextBob, String script) {
    // TODO: check if the compiled javascript can be cached

    log.debug("Evaluating script for " + contextBob + " script: " + script);

    OBScriptEngine engine = OBScriptEngine.getInstance();

    try {
      Check.isNotNull(engine,
          "Scripting engine not found using name js, check for other scripting language names such as Mozilla Rhino");

      final Entity e = contextBob.getEntity();

      Map<String, Object> bindings = new HashMap<>();
      for (final Property p : e.getProperties()) {
        bindings.put(p.getName(), contextBob.get(p.getName()));
      }

      final Object result = engine.eval(script, bindings);
      Check.isInstanceOf(result, Boolean.class);
      return (Boolean) result;
    } catch (final ScriptException e) {
      throw new OBException(
          "Exception while executing " + script + " for business object " + contextBob, e);
    }
  }
}
