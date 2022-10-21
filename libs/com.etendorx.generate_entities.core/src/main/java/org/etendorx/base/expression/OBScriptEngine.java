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

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Class that wraps a ScriptEngine and that should be used to evaluate javascript scripts
 * <p>
 * It is a singleton, and it takes advantage of the thread safety of ScriptEngine
 */
public class OBScriptEngine {

  private final ScriptEngine engine;

  private static OBScriptEngine instance = new OBScriptEngine();

  public static OBScriptEngine getInstance() {
    return instance;
  }

  private OBScriptEngine() {
    ScriptEngineManager manager = new ScriptEngineManager();
    engine = manager.getEngineByName("rhino");
  }

  public Object eval(String script) throws ScriptException {
    return eval(script, Collections.emptyMap());
  }

  public Object eval(String script, Map<String, Object> properties) throws ScriptException {
    Bindings bindings = engine.createBindings();
    copyPropertiesToBindings(properties, bindings);
    Object result = engine.eval(script, bindings);
    // Sometimes rhino evaluates to "undefined" when it should evaluate to null
    // This transforms all undefined results to null
    // Related issue: https://github.com/mozilla/rhino/issues/760
    if ("undefined".equals(result)) {
      return null;
    }
    return result;
  }

  private void copyPropertiesToBindings(Map<String, Object> properties, Bindings bindings) {
    for (Entry<String, Object> entry : properties.entrySet()) {
      bindings.put(entry.getKey(), entry.getValue());
    }
  }

}
