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

package org.etendorx.service.db;

import org.etendorx.base.exception.OBException;
import org.etendorx.base.provider.OBSingleton;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.BatchUpdateException;
import java.sql.SQLException;

/**
 * Utility class with very general utility methods.
 *
 * @author mtaal
 */

public class DbUtility implements OBSingleton {

  /**
   * This method will take care of finding the real underlying exception. When a jdbc or hibernate
   * exception occurs then the whole stack trace is not available in the log because the exception
   * does not return the underlying exception using the {@link Throwable#getCause()} but using the
   * {@link SQLException#getNextException()}.
   *
   * @param throwable
   *   the throwable to analyze
   *
   * @return the underlying sql exception or the original throwable if none found
   */
  public static Throwable getUnderlyingSQLException(Throwable throwable) {

    if (throwable instanceof BatchUpdateException && ((BatchUpdateException) throwable).getNextException() != null) {
      return ((BatchUpdateException) throwable).getNextException();
    }
    if (throwable.getCause() instanceof BatchUpdateException && ((BatchUpdateException) throwable.getCause()).getNextException() != null) {
      final BatchUpdateException bue = (BatchUpdateException) throwable.getCause();
      return bue.getNextException();
    }
    if (throwable.getCause() instanceof org.hibernate.exception.GenericJDBCException && ((org.hibernate.exception.GenericJDBCException) throwable.getCause()).getSQLException()
      .getNextException() != null) {
      final org.hibernate.exception.GenericJDBCException gjdbce = (org.hibernate.exception.GenericJDBCException) throwable.getCause();
      return gjdbce.getSQLException().getNextException();
    }
    if (throwable.getCause() instanceof org.hibernate.exception.ConstraintViolationException && ((org.hibernate.exception.ConstraintViolationException) throwable.getCause()).getSQLException()
      .getNextException() != null) {
      final org.hibernate.exception.ConstraintViolationException cve = (org.hibernate.exception.ConstraintViolationException) throwable.getCause();
      return cve.getSQLException().getNextException();
    }

    if (throwable.getCause() instanceof SQLException) {
      return throwable.getCause();
    }

    return throwable;
  }

  /**
   * Reads a file and returns the content as a String. The file must exist otherwise an
   * {@link OBException} is thrown.
   *
   * @param file
   *   the file to read
   *
   * @return the content of the file
   */
  public static String readFile(File file) {
    final StringBuilder contents = new StringBuilder();

    try {
      final BufferedReader input = new BufferedReader(new FileReader(file));
      try {
        String line = null;
        while ((line = input.readLine()) != null) {
          contents.append(line);
          contents.append(System.getProperty("line.separator"));
        }
      } finally {
        input.close();
      }
    } catch (final IOException e) {
      throw new OBException(e);
    }
    return contents.toString();
  }
}
