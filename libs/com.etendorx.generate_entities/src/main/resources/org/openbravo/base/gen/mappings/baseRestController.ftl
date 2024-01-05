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

import com.etendorx.entities.mapper.lib.BindedRestController;
import com.etendorx.entities.mapper.lib.DASRepository;
import com.etendorx.entities.mapper.lib.JsonPathConverter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/${mappingPrefix?lower_case}/${entity.externalName}")
public class ${mappingPrefix}${entity.externalName}RestController extends BindedRestController<${mappingPrefix}${entity.externalName}DTORead, ${mappingPrefix}${entity.externalName}DTOWrite> {

  public ${mappingPrefix}${entity.externalName}RestController(
  JsonPathConverter<${mappingPrefix}${entity.externalName}DTOWrite> converter,
  @Autowired @Qualifier("${mappingPrefix}${entity.externalName}DASRepository") DASRepository<${mappingPrefix}${entity.externalName}DTORead, ${mappingPrefix}${entity.externalName}DTOWrite> repository) {
    super(converter, repository);
  }

  <#list modelProviderRX.getETRXRepositories(entity) as repo>
    <#list repo.searches as search>
  @GetMapping("/searches/${search.method}")
  @Operation(security = { @SecurityRequirement(name = "basicScheme") })
  @Transactional
  Page<${mappingPrefix}${entity.externalName}DTORead> ${search.method}(<#list search.params as param>${param.type} ${param.name}, </#list> @PageableDefault(size = 20) final Pageable pageable) {
    return ((${mappingPrefix}${entity.externalName}DTORepositoryDefault)getRepository()).${search.method}(<#list search.params as param>${param.name}, </#list> pageable);
  }
    </#list>
  </#list>

}
