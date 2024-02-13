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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import com.etendorx.entities.entities.mappings.MappingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import com.etendorx.entities.mapper.lib.JsonPathConverterBase;
import com.etendorx.entities.mapper.lib.JsonPathEntityRetriever;

@Component
@Slf4j
public class ${mappingPrefix}${entity.externalName}JsonPathConverter extends JsonPathConverterBase<${mappingPrefix}${entity.externalName}DTOWrite> {

  private final MappingUtils mappingUtils;
  <#list objectFields as field>
  <#if field.property??>
    <#assign columnType = modelProvider.getColumnTypeFullQualified(entity.table, entity.table.name + "." + field.property) ! "" />
    <#if columnType?? && columnType != "">
  private final JsonPathEntityRetriever<${columnType}> ${field.name}Retriever;
    </#if>
  </#if>
  </#list>

  public ${mappingPrefix}${entity.externalName}JsonPathConverter(
    MappingUtils mappingUtils<#if objectFields?size gt 0>,</#if>
<#list objectFields as field>
  <#if field.property??>
    <#assign columnType = modelProvider.getColumnTypeFullQualified(entity.table, entity.table.name + "." + field.property) ! "" />
    <#if field.etrxProjectionEntityRelated??>
      <#assign targetEntityName = field.etrxProjectionEntityRelated.externalName />
    <#else>
      <#assign targetEntityName = modelProvider.getColumnEntityName(entity.table, entity.table.name + "." + field.property) ! "" />
    </#if>
    <#if columnType?? && columnType != "">
    ${mappingPrefix}${targetEntityName}JsonPathRetriever ${field.name}Retriever<#if field_has_next>,</#if>
    </#if>
  </#if>
</#list>
  ) {
    super();
    this.mappingUtils = mappingUtils;
  <#list objectFields as field>
    this.${field.name}Retriever = ${field.name}Retriever;
  </#list>
  }

  @Override
  public ${mappingPrefix}${entity.externalName}DTOWrite convert(String rawData) {
    var ctx = getReadContext(rawData);
    List<String> missingFields = new ArrayList<>();
    List<String> expectedFields = new ArrayList<>();
    Map<String, Object> json = ctx.json();
    Set<String> receivedFields = json.keySet();

    Function<String, Void> missing = (String path) -> {
      missingFields.add(path);
      return null;
    };

    ${mappingPrefix}${entity.externalName}DTOWrite dto = new ${mappingPrefix}${entity.externalName}DTOWrite();
  <#list entity.fields as field>
  <#assign hasRetriever = false />
  <#if field.property??>
    <#assign columnType = modelProvider.getColumnTypeFullQualified(entity.table, entity.table.name + "." + field.property) ! "" />
    <#if columnType?? && columnType != "">
      <#assign hasRetriever = true />
    </#if>
  </#if>
  <#assign returnClass = ""/>
  log.debug("-- Parsing {}", "${field.name}");
  expectedFields.add("${field.jsonPath!"$."+field.name}");
  <#if field.fieldMapping == "CM">
    dto.set<@toCamelCase field.name />(mappingUtils.constantValue("${field.constantValue.id}"));
  <#else>
    <#if hasRetriever>
      <#if field.constantValue??>
    var _${NamingUtil.getSafeJavaName(field.name)}> = Optional.of(retrieve${field.name?cap_first}(
      mappingUtils.constantValue("${field.constantValue.id}")
    );
      <#else>
    var val${field.name} = getData(ctx, "${field.jsonPath!"$."+field.name}", String.class, missing);
    var _${NamingUtil.getSafeJavaName(field.name)} = val${field.name}.map(this::retrieve${field.name?cap_first});
      </#if>
    <#elseif field.property??>
      <#assign returnClass = modelProvider.getColumnPrimitiveType(entity.table, entity.table.name + "." + field.property) ! "" />
    var _${NamingUtil.getSafeJavaName(field.name)} = getData(ctx, "${field.jsonPath!"$."+field.name}"<#if returnClass != "">, <#if returnClass == "java.util.Date">String<#else>${returnClass}</#if>.class</#if>, missing);
    <#else>
    var _${NamingUtil.getSafeJavaName(field.name)} = getData(ctx, "${field.jsonPath!"$."+field.name}", Object.class, missing);
    </#if>
    log.debug("pathConverter ${entity.externalName} \"${field.jsonPath!"$."+field.name}\": {}", _${NamingUtil.getSafeJavaName(field.name)});
    dto.set<@toCamelCase field.name />(
    <#if returnClass=="java.util.Date">
      mappingUtils.parseDate(_${NamingUtil.getSafeJavaName(field.name)}.orElse(null))
    <#else>
      _${NamingUtil.getSafeJavaName(field.name)}.orElse(null)
    </#if>
    );
  </#if>
  log.debug("// End Parsing {}", "${field.name}");
  </#list>
    // Debug missing fields
    log.debug("missing fields: {}", debugFields(expectedFields, receivedFields, missingFields));
    return dto;
  }

<#list entity.fields as field>
  <#if field.property??>
    <#assign columnType = modelProvider.getColumnTypeFullQualified(entity.table, entity.table.name + "." + field.property) ! "" />
    <#if columnType?? && columnType != "">
  private ${columnType} retrieve${field.name?cap_first}(Object id) {
    return ${field.name}Retriever.get(id);
  }

    </#if>
  </#if>
</#list>

}
