<#macro toCamelCase string>
  <#compress>
    <#if string?matches("^[a-z]+[A-Za-z0-9]*$")>${string?cap_first}<#else>
      <#assign result="">
      <#list string?split("_") as part>
        <#if part?index == 0>
          <#assign result = result + part>
        <#else>
          <#assign result = result + part?cap_first>
        </#if>
      </#list>${result?cap_first}</#if>
  </#compress>
</#macro>
<#macro convertToGetMethod path>
  <#compress>
  <#assign result="entity">
  <#list path?split(".") as part>
    <#assign result = result + ".get" + part?cap_first + "()">
  </#list>
  ${result}
  </#compress>
</#macro>
<#assign javaMappings = []>
<#list entity.fields as field>
  <#if field.name != "id" && field.fieldMapping == "JM">
    <#assign javaMappings = javaMappings + [field]>
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

import com.etendorx.entities.mapper.lib.DTOWriteMapping;
import ${entity.table.thePackage.javaPackage}.${entity.table.className};
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ${mappingPrefix}${entity.name}FieldConverterWrite {

<#list javaMappings as field>
  private final DTOWriteMapping<${entity.table.className}, ${mappingPrefix}${entity.name}DTOWrite> ${field.name};
</#list>

  public ${mappingPrefix}${entity.name}FieldConverterWrite(
<#list javaMappings as field>
    @Qualifier("${field.javaMapping.qualifier}") @Autowired DTOWriteMapping<${entity.table.className}, ${mappingPrefix}${entity.name}DTOWrite> ${field.name}<#if field_has_next>,</#if>
</#list>
  ) {
    super();
<#list javaMappings as field>
    this.${field.name} = ${field.name};
</#list>
  }

  public String getId(${mappingPrefix}${entity.name}DTOWrite entity) {
    return entity.getId();
  }

<#list javaMappings as field>
  public void set<@toCamelCase field.name/>(${entity.table.className} entity, ${mappingPrefix}${entity.name}DTOWrite dto) {
    ${field.name}.map(entity, dto);
  }

</#list>
<#list entity.fields as field>
  <#if field.name != "id" && (field.fieldMapping == "DM" || field.fieldMapping == "EM")>
  public void set<@toCamelCase field.name/>(${entity.table.className} entity, ${mappingPrefix}${entity.name}DTOWrite dto) {
    <#if field.property??>
    entity.set${NamingUtil.getSafeJavaName(field.property)?cap_first}(dto.get${field.name?cap_first}());
    <#else>
    entity.set${field.name?cap_first}(dto.get${field.name?cap_first}());
    </#if>
  }
  </#if>

</#list>
}
