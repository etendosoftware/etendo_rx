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

package com.etendorx.entities.utilities;

import lombok.SneakyThrows;
import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;

import java.io.Serializable;

public class UUIDGenerator extends org.hibernate.id.UUIDGenerator {

  @SneakyThrows @Override
  public Serializable generate(SharedSessionContractImplementor session, Object obj) throws
      HibernateException {
    String result = ((String) super.generate(session, obj)).toUpperCase();
    result = result.replace("-", "");
    if (result.length() != 32) {
      throw new Exception("Generating UUID of wrong length: " + result);
    }
    return result;
  }
}
