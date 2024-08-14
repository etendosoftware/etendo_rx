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

package org.etendorx.dal.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.etendorx.base.exception.OBException;

import javax.servlet.ServletException;

/**
 * A convenience class which can be used as a base class for when specific actions need to be done
 * before or after a thread has run. cleaning up certain threadlocals.
 *
 * @author mtaal
 */

public abstract class ThreadHandler {
  private static final Logger log = LogManager.getLogger();

  /**
   * Run the thread, this method will call the protected methods doBefore, doAction and doFinal.
   */
  public void run() {
    boolean err = true;
    try {
      log.debug("Thread started --> doBefore");
      doBefore();
      log.debug("Thread --> doAction");
      doAction();
      log.debug("Thread --> Action done");
      err = false;
      // TODO add exception logging/tracing/emailing
      // } catch (Throwable t) {
      // ExceptionHandler.reportThrowable(t, (HttpServletRequest)
      // request);
      // throw new ServletException(t);
    } catch (final ServletException se) {
      if (se.getRootCause() != null) {
        throw new OBException("Exception thrown " + se.getRootCause().getMessage(),
            se.getRootCause());
      } else {
        throw new OBException("Exception thrown " + se.getMessage(), se);
      }
    } catch (final Exception t) {
      log.error(t.getMessage(), t);
      throw new OBException("Exception thrown " + t.getMessage(), t);
    } finally {
      doFinal(err);
    }
  }

  protected abstract void doBefore() throws Exception;

  protected abstract void doFinal(boolean errorOccured);

  protected abstract void doAction() throws Exception;
}
