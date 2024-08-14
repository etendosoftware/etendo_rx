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

package com.etendorx.asyncprocess;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main class to launch async process service
 */
@SpringBootApplication
@ComponentScan(basePackages = { "com.etendorx.lib.asyncprocess.utils", "com.etendorx.lib.kafka",
    "com.etendorx.lib.kafka.configuration", "com.etendorx.lib.kafka.topology",
    "com.etendorx.asyncprocess", "com.etendorx.asyncprocess.controller",
    "com.etendorx.utils.auth.key.context" })
@OpenAPIDefinition(info = @Info(title = "Async Process API", version = "1.0"))
@SecurityScheme(name = "javainuseapi", scheme = "basic", type = SecuritySchemeType.HTTP, in = SecuritySchemeIn.HEADER)
public class AsyncProcessDbApp {

  public static void main(String[] args) {
    SpringApplication.run(AsyncProcessDbApp.class, args);
  }
}
