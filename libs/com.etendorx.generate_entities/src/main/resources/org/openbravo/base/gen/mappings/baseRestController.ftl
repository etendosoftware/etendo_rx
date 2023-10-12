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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/${mappingPrefix?lower_case}/${entity.name}")
public class ${mappingPrefix}${entity.name}RestController extends BindedRestController<${mappingPrefix}${entity.name}DTORead, ${mappingPrefix}${entity.name}DTOWrite> {

  public ${mappingPrefix}${entity.name}RestController(
  JsonPathConverter<${mappingPrefix}${entity.name}DTOWrite> converter,
  @Autowired @Qualifier("${mappingPrefix}${entity.name}DASRepository") DASRepository<${mappingPrefix}${entity.name}DTORead, ${mappingPrefix}${entity.name}DTOWrite> repository) {
    super(converter, repository);
  }

}
