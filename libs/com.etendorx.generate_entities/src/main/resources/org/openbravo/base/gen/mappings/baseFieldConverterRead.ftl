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
<#macro convertToGetMethod name path mappingType>
  <#assign result="entity">
  <#assign nullCheck="if (">
  <#list path?split(".") as part>
    <#if part?index != 0>
      <#assign nullCheck = nullCheck + " && ">
    </#if>
    <#assign result = result + ".get" + part?cap_first + "()">
    <#assign nullCheck = nullCheck + result + " != null">
  </#list>
  <#if mappingType == "EM">
    <#assign result = name + ".convert(" + result + ")">
  </#if>
  <#assign nullCheck = nullCheck + ") {\n      return mappingUtils.handleBaseObject(" + result + ");\n    } else {\n      return null;\n    }">
  ${nullCheck}
</#macro>
<#macro getConverterName field>
  <#assign projectionName = field.etrxProjectionEntityRelated.projection.name?upper_case>
  <#assign tableName = field.etrxProjectionEntityRelated.table.name>
  <#compress>
  ${projectionName}${tableName}DTOConverter
  </#compress>
</#macro>
<#assign mappings = []>
<#list entity.fields as field>
  <#if field.name != "id" && (field.fieldMapping == "JM" || field.fieldMapping == "EM")>
    <#assign mappings = mappings + [field]>
  </#if>
</#list>
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

import com.etendorx.entities.entities.mappings.MappingUtils;
import com.etendorx.entities.mapper.lib.DTOReadMapping;
import ${entity.table.thePackage.javaPackage}.${entity.table.className};
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ${mappingPrefix}${entity.name}FieldConverterRead {

  private final MappingUtils mappingUtils;
<#list mappings as field>
  <#if field.fieldMapping == "JM">
  private final DTOReadMapping<${entity.table.className}> ${field.name};
  <#else>
  private final <@getConverterName field=field/> ${field.name};
  </#if>
</#list>

  public ${mappingPrefix}${entity.name}FieldConverterRead(
        MappingUtils mappingUtils<#if mappings?size gt 0>,</#if>
<#compress>
  <#list mappings as field>
    <#compress>
    <#if field.fieldMapping == "JM">
    @Qualifier("${field.javaMapping.qualifier}") @Autowired DTOReadMapping<${entity.table.className}> ${field.name}
    <#else>
    @Autowired <@getConverterName field=field/> ${field.name}
    </#if><#if field_has_next>,</#if>
    </#compress>
  </#list>
</#compress>
  ) {
    super();
    this.mappingUtils = mappingUtils;
<#list mappings as field>
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
  <#if field.name != "id" && (field.fieldMapping == "DM" || field.fieldMapping == "EM") && field.entity.mappingType == "R">
  public Object get<@toCamelCase field.name/>(${entity.table.className} entity) {
    // ${field.property}
    <@convertToGetMethod field.name field.property field.fieldMapping/>
  }
  </#if>

</#list>
}
