/*
 *************************************************************************
 * The contents of this file are subject to the Openbravo  Public  License
 * Version  1.1  (the  "License"),  being   the  Mozilla   Public  License
 * Version 1.1  with a permitted attribution clause; you may not  use this
 * file except in compliance with the License. You  may  obtain  a copy of
 * the License at http://www.openbravo.com/legal/license.html
 * Software distributed under the License  is  distributed  on  an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific  language  governing  rights  and  limitations
 * under the License.
 * The Original Code is Openbravo ERP.
 * The Initial Developer of the Original Code is Openbravo SLU
 * All portions are Copyright (C) 2019 Openbravo SLU
 * All Rights Reserved.
 * Contributor(s):  ______________________________________.
 ************************************************************************
 */

package com.etendorx.auth.auth.hashing;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Handles hashing passwords to be stored in database supporting different
 * {@link HashingAlgorithm}s.
 *
 * @since 21Q4
 */
public class PasswordHash {
  private static final Logger log = LogManager.getLogger();
  private static final int DEFAULT_CURRENT_ALGORITHM_VERSION = 1;

  private static final Map<Integer, HashingAlgorithm> ALGORITHMS;

  static {
    ALGORITHMS = new HashMap<>(2);
    ALGORITHMS.put(0, new SHA1());
    ALGORITHMS.put(1, new SHA512Salt());
  }

  private PasswordHash() {
  }

  /**
   * Generates a hash for the {@code plainText} using current default {@link HashingAlgorithm}
   */
  public static String generateHash(String plainText) {
    return ALGORITHMS.get(DEFAULT_CURRENT_ALGORITHM_VERSION).generateHash(plainText);
  }

  /**
   * Checks whether a plain text password matches with a hashed password
   */
  public static boolean matches(String plainTextPassword, String hashedPassword) {
    HashingAlgorithm algorithm = getAlgorithm(hashedPassword);
    log.trace("Checking password with algorithm {}", () -> algorithm.getClass().getSimpleName());
    return algorithm.check(plainTextPassword, hashedPassword);
  }

  /**
   * Determines the algorithm used to hash a given password.
   */
  static HashingAlgorithm getAlgorithm(String hash) {
    HashingAlgorithm algorithm = ALGORITHMS.get(getVersion(hash));

    if (algorithm == null) {
      throw new IllegalStateException(
          "Hashing algorithm version " + getVersion(hash) + " is not implemented");
    }

    return algorithm;
  }

  private static int getVersion(String hash) {
    int idx = hash.indexOf('$');
    if (idx == -1) {
      return 0;
    }
    return Integer.parseInt(hash.substring(0, idx));
  }
}
