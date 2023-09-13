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
<#assign objectFields = []>
<#list entity.fields as field>
  <#if field.property??>
    <#assign columnType = modelProvider.getColumnTypeFullQualified(entity.table, entity.table.name + "." + field.property) ! "" />
    <#if columnType?? && columnType != "">
      <#assign objectFields = objectFields + [field]>
    </#if>
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

import java.math.BigDecimal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.etendorx.entities.mapper.lib.JsonPathConverterBase;
import com.etendorx.entities.mapper.lib.JsonPathEntityRetriever;

@Component
@Slf4j
public class ${mappingPrefix}${entity.name}JsonPathConverter extends JsonPathConverterBase<${mappingPrefix}${entity.name}DTOWrite> {

  <#list objectFields as field>
  <#if field.property??>
    <#assign columnType = modelProvider.getColumnTypeFullQualified(entity.table, entity.table.name + "." + field.property) ! "" />
    <#if columnType?? && columnType != "">
  private final JsonPathEntityRetriever<${columnType}> ${field.name}Retriever;
    </#if>
  </#if>
  </#list>

  public ${mappingPrefix}${entity.name}JsonPathConverter(
<#list objectFields as field>
  <#if field.property??>
    <#assign columnType = modelProvider.getColumnTypeFullQualified(entity.table, entity.table.name + "." + field.property) ! "" />
    <#assign targetEntityName = modelProvider.getColumnEntityName(entity.table, entity.table.name + "." + field.property) ! "" />
    <#if columnType?? && columnType != "">
    ${mappingPrefix}${targetEntityName}JsonPathRetriever ${field.name}Retriever<#if field_has_next>,</#if>
    </#if>
  </#if>
</#list>
  ) {
    super();
  <#list objectFields as field>
    this.${field.name}Retriever = ${field.name}Retriever;
  </#list>
  }

  @Override
  public ${mappingPrefix}${entity.name}DTOWrite convert(String rawData) {
    var ctx = getReadContext(rawData);

    ${mappingPrefix}${entity.name}DTOWrite dto = new ${mappingPrefix}${entity.name}DTOWrite();
  <#list entity.fields as field>
  <#assign hasRetriever = false />
  <#if field.property??>
    <#assign columnType = modelProvider.getColumnTypeFullQualified(entity.table, entity.table.name + "." + field.property) ! "" />
    <#if columnType?? && columnType != "">
      <#assign hasRetriever = true />
    </#if>
  </#if>
  <#if hasRetriever>
    var ${NamingUtil.getSafeJavaName(field.name)} = retrieve${field.name?cap_first}(ctx.read("${field.jsonPath!"missing json path"}"));
  <#elseif field.property??>
    <#assign returnClass = modelProvider.getColumnPrimitiveType(entity.table, entity.table.name + "." + field.property) ! "" />
    var ${NamingUtil.getSafeJavaName(field.name)} = ctx.read("${field.jsonPath!"missing json path"}"<#if returnClass != "">, ${returnClass}.class</#if>);
  <#else>
    var ${NamingUtil.getSafeJavaName(field.name)} = ctx.read("${field.jsonPath!"missing json path"}");
  </#if>
    log.debug("pathConverter ${entity.name} \"${field.jsonPath!"missing json path"}\": {}", ${NamingUtil.getSafeJavaName(field.name)});
    dto.set<@toCamelCase field.name />(${NamingUtil.getSafeJavaName(field.name)});
  </#list>
    return dto;
  }

<#list entity.fields as field>
  <#if field.property??>
    <#assign columnType = modelProvider.getColumnTypeFullQualified(entity.table, entity.table.name + "." + field.property) ! "" />
    <#if columnType?? && columnType != "">
  private ${columnType} retrieve${field.name?cap_first}(String id) {
    if (id == null) {
      return null;
    }
    return ${field.name}Retriever.get(id);
  }

    </#if>
  </#if>
</#list>

}
