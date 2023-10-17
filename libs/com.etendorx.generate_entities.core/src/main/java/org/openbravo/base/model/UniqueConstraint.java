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

package org.openbravo.base.model;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.etendorx.base.util.Check;

import java.util.ArrayList;
import java.util.List;

/**
 * A UniqueConstraint defines for an entity a set of properties, which combined are unique for that
 * entity.
 *
 * @author mtaal
 */

public class UniqueConstraint {
  private static final Logger log = LogManager.getLogger();

  private List<Property> properties = new ArrayList<Property>();
  private String name;
  private Entity entity;
  private boolean invalid = false;

  protected void addPropertyForColumn(String columnName) {
    if (isInvalid()) {
      return;
    }
    for (final Property property : entity.getProperties()) {
      // one-to-many properties have a null columnname
      if (property.getColumnName() != null && property.getColumnName()
        .equalsIgnoreCase(columnName)) {
        Check.isFalse(properties.contains(property),
          "Column " + columnName + " occurs twice in uniqueconstraint " + name + " in entity " + entity + " table " + entity.getTableName());
        properties.add(property);
        log.debug("Adding property {} to uniqueconstraint {}", property, name);
        return;
      }
    }

    setInvalid(true);
    log.error(
      "Fail when setting uniqueconstraint {} columnname {} not present in entity {} table {}. Ignoring this unique constraint",
      getName(), columnName , entity , entity.getTableName()
    );
    entity.getUniqueConstraints().remove(this);
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    log.debug("Created unique constraint {}", name);
    this.name = name;
  }

  public Entity getEntity() {
    return entity;
  }

  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  public List<Property> getProperties() {
    return properties;
  }

  public boolean isInvalid() {
    return invalid;
  }

  public void setInvalid(boolean invalid) {
    this.invalid = invalid;
  }
}
