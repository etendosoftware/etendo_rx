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

import org.etendorx.base.exception.OBException;

/**
 * Is thrown by the {@link Check Check} invariant check methods. Unchecked state exception which
 * also logs itself.
 *
 * @author mtaal
 */
public class CheckException extends OBException {

  private static final long serialVersionUID = 1L;

  public CheckException() {
    super();
  }

  public CheckException(String message, Throwable cause) {
    super(message, cause);
  }

  public CheckException(String message) {
    super(message);
  }

  public CheckException(Throwable cause) {
    super(cause);
  }
}
