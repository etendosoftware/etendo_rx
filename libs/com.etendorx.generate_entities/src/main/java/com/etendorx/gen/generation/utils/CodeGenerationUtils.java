/*
 * Copyright 2022-2023  Futit Services SL
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
package com.etendorx.gen.generation.utils;

import com.etendorx.gen.generation.GeneratePaths;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class CodeGenerationUtils {
  public static Writer getWriter(String mappingPrefix, String outFileName,
      GeneratePaths path) throws FileNotFoundException {
    final String packageJPARepo = path.pathEntitiesRx.substring(path.pathEntitiesRx.lastIndexOf('/')) + ".mappings";
    final String fullPathJPARepo = path.pathEntitiesRx + "/src/main/mappings/" + packageJPARepo.replace('.', '/');
    final String repositoryClass = mappingPrefix + outFileName;
    //
    new File(fullPathJPARepo).mkdirs();
    var outFileRepo = new File(fullPathJPARepo, repositoryClass);

    return new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(outFileRepo), StandardCharsets.UTF_8));
  }

}
