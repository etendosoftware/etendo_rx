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

package org.etendorx.base.validation;

import org.etendorx.base.exception.OBException;
import org.openbravo.base.model.Property;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Is thrown when an entity or property value is invalid. This Exception is not logged. Instead it
 * allows messages to be added and stored by {@link Property Property}.
 *
 * @author mtaal
 */
public class ValidationException extends OBException {

  private static final long serialVersionUID = 1L;

  private Map<Property, String> msgs = new HashMap<>();

  public ValidationException() {
    super();
  }

  public void addMessage(Property p, String msg) {
    msgs.put(p, msg);
  }

  public boolean hasMessages() {
    return !msgs.isEmpty();
  }

  @Override public String getMessage() {
    if (msgs == null) {
      // during construction
      return "";
    }

    return msgs.entrySet()
        .stream()
        .map(msgEntry -> msgEntry.getKey().getEntity().getName() + "." + msgEntry.getKey()
            .getName() + ": " + msgEntry.getValue())
        .collect(Collectors.joining("\n"));
  }
}
