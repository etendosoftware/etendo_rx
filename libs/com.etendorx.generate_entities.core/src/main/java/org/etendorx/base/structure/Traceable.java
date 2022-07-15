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

//import org.openbravo.model.ad.access.User;

import java.util.Date;

/**
 * An interface modeling open bravo objects which have audit info fields such as created, createdBy,
 * etc.
 *
 * @author mtaal
 */

public interface Traceable {

  /**
   * Created by audit user
   *
   * @return User
   */
  //public User getCreatedBy();

  /**
   * Created by audit user
   *
   * @param user
   */
  //public void setCreatedBy(User user);

  /**
   * Creation date of audit
   *
   * @return Date of creation
   */
  public Date getCreationDate();

  /**
   * Creation date of audit
   *
   * @param date
   */
  public void setCreationDate(Date date);

  /**
   * Update by audit user
   *
   * @return User who updated
   */
  //  public User getUpdatedBy();

  /**
   * Update by audit user
   *
   * @param user
   */
  //public void setUpdatedBy(User user);

  /**
   * Update date of audit
   *
   * @return Date of update
   */
  public Date getUpdated();

  /**
   * Update date of audit
   *
   * @param date
   */
  public void setUpdated(Date date);
}
