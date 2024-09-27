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
<#function firstProperty property>
  <#list property?split(".") as part>
    <#return part>
  </#list>
</#function>
/**
* Copyright 2022-2024 Futit Services SL
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
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

public class ${mappingPrefix}${entity.externalName}DTO<#if entity.mappingType == "R">Read<#else>Write</#if>Request implements BaseDTOModel {
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
      <#if !field.property?? || field.isMandatory>
  @NotNull
      </#if>
      <#assign columnType = "String">
      <#if field.property??>
        <#if field.fieldMapping == "EM" && genUtils.isOneToMany(field)>
          <#assign columnType = "List<" + genUtils.getDto(field, entity.mappingType) + "Request>"/>
        <#elseif field.fieldMapping == "EM" && field.createRelated?? && field.createRelated>
          <#assign columnType = genUtils.getDto(field, entity.mappingType) + "Request" />
        <#else>
          <#assign columnType = "String" />
        </#if>
      </#if>
  <#if entity.mappingType == "R">String<#else>${columnType}</#if> <@toCamelCase field.name/>;
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
