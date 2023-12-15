<#macro toCamelCase string>
  <#compress>
    <#assign finalResultList = []>
    <#list string?split(".") as subString>
      <#assign isFirstSubStr = (subString?index == 0)>
      <#assign result="">
      <#list subString?split("_") as part>
        <#if part?index == 0>
          <#assign result = result + part>
        <#else>
          <#assign result = result + part?cap_first>
        </#if>
      </#list>
      <#if isFirstSubStr>
        <#assign finalResultList = finalResultList + [NamingUtil.getSafeJavaName(result)]>
      <#else>
        <#assign finalResultList = finalResultList + [NamingUtil.getSafeJavaName(result)?cap_first]>
      </#if>
    </#list>
    ${finalResultList?join("")}
  </#compress>
</#macro>
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

import com.etendorx.entities.mapper.lib.BaseDTOModel;
<#if entity.mappingType == "R">
import com.fasterxml.jackson.annotation.JsonProperty;
</#if>
import lombok.Getter;
import lombok.Setter;

  <#list entity.fields as field>
    <#if field.property??>
      <#assign columnType = modelProvider.getColumnTypeFullQualified(entity.table, entity.table.name + "." + field.property) ! "" />
      <#if columnType?? && columnType != "">
import ${columnType};

      </#if>
    </#if>
  </#list>

public class ${mappingPrefix}${entity.name}DTO<#if entity.mappingType == "R">Read<#else>Write</#if> implements BaseDTOModel {
  <#if entity.mappingType == "R">
  @JsonProperty("id")
  </#if>
  @Getter @Setter
  String id;

  <#list entity.fields as field>
    <#if field.name != "id">
      <#if entity.mappingType == "R">
  @JsonProperty("${field.name}")
      </#if>
      <#if !NamingUtil.isJavaReservedWord(field.name)>
  @Getter @Setter
      </#if>
      <#assign columnType = "Object">
      <#if field.property??>
        <#assign columnType = modelProvider.getColumnTypeName(entity.table, entity.table.name + "." + field.property) ! "Object">
      </#if>
  <#if entity.mappingType == "R">Object<#else>${columnType}</#if> <@toCamelCase field.name/>;
      <#if NamingUtil.isJavaReservedWord(field.name)>
        <#assign safeFieldName=NamingUtil.getSafeJavaName(field.name) >

  public <#if entity.mappingType == "R">Object<#else>${columnType}</#if> get${field.name?cap_first}() {
    return this.${safeFieldName};
  }

  public void set${field.name?cap_first}(<#if entity.mappingType == "R">Object<#else>${columnType}</#if> ${safeFieldName}) {
    this.${safeFieldName} = ${safeFieldName};
  }

      </#if>

    </#if>
  </#list>
}
