<#macro toCamelCase string>
  <#compress>
    <#assign finalResultList = []>
    <#list string?split(".") as subString>
      <#assign isFirstSubStr = (subString?index == 0)>
      <#assign result="">
      <#list subString?split("_") as part>
        <#assign result = result + part?cap_first>
      </#list>
      <#if isFirstSubStr>
        <#assign finalResultList = finalResultList + [result]>
      <#else>
        <#assign finalResultList = finalResultList + [result?cap_first]>
      </#if>
    </#list>
    ${finalResultList?join("")}
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

import com.etendorx.entities.mapper.lib.DTOReadMapping;
import ${entity.table.thePackage.javaPackage}.${entity.table.className};
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ${mappingPrefix}${entity.name}FieldConverterRead {

<#list javaMappings as field>
  private final DTOReadMapping<${entity.table.className}> ${field.name};
</#list>

  public ${mappingPrefix}${entity.name}FieldConverterRead(
<#list javaMappings as field>
    @Qualifier("${field.javaMapping.qualifier}") @Autowired DTOReadMapping<${entity.table.className}> ${field.name}<#if field_has_next>,</#if>
</#list>
  ) {
    super();
<#list javaMappings as field>
    this.${field.name} = ${field.name};
</#list>
  }

  public String getId(${entity.table.className} entity) {
    return entity.getId();
  }

<#list javaMappings as field>
  public Object get<@toCamelCase field.name/>(${entity.table.className} entity) {
    return ${field.name}.map(entity);
  }

</#list>

<#list entity.fields as field>
  <#if field.name != "id" && field.fieldMapping == "DM" && field.entity.mappingType == "R">
  public Object get<@toCamelCase field.name/>(${entity.table.className} entity) {
    // ${field.property}
    return <@convertToGetMethod field.property/>;
  }
  </#if>

</#list>
}
