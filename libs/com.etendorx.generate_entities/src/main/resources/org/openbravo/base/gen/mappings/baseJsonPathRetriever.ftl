<#assign indentifierFields = []>
<#list entity.fields as field>
  <#if field.identifiesUnivocally>
    <#assign indentifierFields = indentifierFields + [field]>
  </#if>
</#list>
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

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Component;
import ${entity.table.thePackage.javaPackage}.${entity.table.className};
import com.etendorx.entities.jparepo.${entity.table.name}Repository;

import com.etendorx.entities.mapper.lib.JsonPathEntityRetrieverBase;

@Component
@Slf4j
public class ${mappingPrefix}${entity.externalName}JsonPathRetriever extends JsonPathEntityRetrieverBase<${entity.table.className}> {

  private final JpaSpecificationExecutor<${entity.table.className}> repository;

  public ${mappingPrefix}${entity.externalName}JsonPathRetriever(${entity.table.name}Repository repository) {
    this.repository = repository;
  }

  @Override
  public JpaSpecificationExecutor<${entity.table.className}> getRepository() {
    return repository;
  }

  @Override
  public String[] getKeys() {
    var keys = new String[]{
    <#list indentifierFields as field>
      "${field.property}"<#if field_has_next>,</#if>
    </#list>
    };
    log.debug("retrieve ${entity.externalName} with keys: {}", (Object) keys);
    return keys;
  }
}
