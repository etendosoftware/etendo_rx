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

package org.etendorx.base.structure;

/**
 * An interface modeling open bravo objects which are identifiable. Identifiable means that a unique
 * id and entityname is available and also a user readable identifier.
 *
 * @author mtaal
 */

public interface Identifiable {

  public Object getId();

  public void setId(Object id);

  public String getEntityName();

  public String getIdentifier();
}
