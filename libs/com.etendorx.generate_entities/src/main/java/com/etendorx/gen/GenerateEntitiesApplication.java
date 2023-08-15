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

package com.etendorx.gen;

import java.io.File;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.config.Configurator;

import com.etendorx.gen.commandline.CommandLineProcess;
import com.etendorx.gen.generation.GenerateEntities;

/**
 * Task generates the entities using the freemarker template engine.
 *
 * @author Sebastian Barrozo
 */
public class GenerateEntitiesApplication {
  private static final Logger log = LogManager.getLogger();

  public static void main(String[] args) {
    Configurator.setRootLevel(Level.INFO);
    new GenerateEntitiesApplication().run(args);
  }

  /**
   * Run the task based on the command line arguments
   * @param args
   */
  public void run(String... args) {
    // Parse args
    CommandLineProcess commandLineProcess = new CommandLineProcess(args);
    final String srcPath = ".";
    boolean friendlyWarnings = false;
    final File baseDir = new File(srcPath);
    var generateEntities = new GenerateEntities();
    generateEntities.setPropertiesFile(baseDir.getAbsolutePath() + File.separator + "gradle.properties");
    generateEntities.setFriendlyWarnings(friendlyWarnings);
    generateEntities.execute(commandLineProcess);
  }

}
