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

package com.etendorx.das;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories("com.etendorx.entities.jparepo")
@ComponentScan(basePackages = { "com.etendorx.utils.auth.key", "com.etendorx.das",
    "com.etendorx.entities.mappings", "com.etendorx.entities.metadata", "com.etendorx.mapping.tutorial", "com.etendorx.openapi",
    "com.etendorx.das.externalid", "com.etendorx.defaultvalues"
})
@ComponentScan(basePackages = "${scan.basePackage:}")
public class EtendorxDasApplication {

  public static void main(String[] args) {
    SpringApplication.run(EtendorxDasApplication.class, args);
  }

}
