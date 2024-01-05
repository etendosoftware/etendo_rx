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

package org.etendorx.base.session;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.dialect.Oracle10gDialect;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.StandardBasicTypes;

import java.sql.Types;

/**
 * Extends the Oracle10Dialect to replace some java-oracle type mappings to support the current rdb
 * schema of OpenBravo. Is used in the {@link SessionFactoryController}.
 *
 * @author mtaal
 */

public class OBOracle10gDialect extends Oracle10gDialect {
  private static final Logger log = LogManager.getLogger();

  public OBOracle10gDialect() {
    super();

    registerHibernateType(Types.NUMERIC, StandardBasicTypes.LONG.getName());
    registerHibernateType(Types.NVARCHAR, StandardBasicTypes.STRING.getName());
    registerHibernateType(Types.NCHAR, StandardBasicTypes.STRING.getName());

    registerColumnType(Types.VARCHAR, 4000, "nvarchar2($l)");
    registerColumnType(Types.VARCHAR, 100, "varchar2($l)");
    registerColumnType(Types.VARCHAR, 5, "char($l)");
    registerFunction("to_number",
        new StandardSQLFunction("to_number", StandardBasicTypes.BIG_DECIMAL));

    log.debug("Created Openbravo specific Oracle DIalect");
  }

}
