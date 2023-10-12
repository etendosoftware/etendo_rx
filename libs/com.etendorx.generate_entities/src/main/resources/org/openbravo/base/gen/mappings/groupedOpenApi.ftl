/**
* Copyright 2022-2023 Futit Services SL
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.etendorx.entities.mappings;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.headers.Header;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

  record Group(String group, String[] pathsToMatch) {}



  @Bean
  public OpenAPI customOpenAPI(@Value("${"$"}{springdoc.version}") String appVersion) {
    return new OpenAPI().components(new Components().addSecuritySchemes("basicScheme",
                new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-TOKEN")
            )
            .addParameters("X-TOKEN",
                new Parameter().in("header").schema(new StringSchema()).name("myHeader1")))
        .info(new Info().title("EtendoRX API")
            .version(appVersion)
            .description(
                "This is an automated API documentation for EtendoRX API. For more information, please visit https://docs.etendo.software")
            .termsOfService("http://swagger.io/terms/")
            .license(new License().name("Apache 2.0").url("http://springdoc.org")));
  }

      <#list projections as projection>
      <#assign projectionName = projection.name?lower_case>
  @Bean
  public GroupedOpenApi ${projectionName}OpenApi() {
    String[] paths = {"/${projectionName}/**"};
    return GroupedOpenApi.builder()
      .group("${projectionName}")
      .pathsToMatch(paths)
      .build();
  }

      </#list>
}
