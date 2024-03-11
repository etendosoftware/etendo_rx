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
<#function firstProperty property>
  <#list property?split(".") as part>
    <#return part>
  </#list>
</#function>
<#function secondProperty property>
    <#list property?split(".") as part>
        <#if part_index == 1>
        <#return part>
        </#if>
    </#list>
    <#return "id">
</#function>
<#assign objectFields = []>
<#list entity.fields as field>
  <#if field.property??>
    <#assign columnType = modelProvider.getColumnTypeFullQualified(entity.table, entity.table.name + "." + firstProperty(field.property)) ! "" />
    <#if columnType?? && columnType != "">
      <#assign objectFields = objectFields + [field]>
    <#elseif field.fieldMapping == "EM" && genUtils.isOneToMany(field)>
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
<#list objectFields as field>
<#if field.fieldMapping == "DM">
<#assign columnType = modelProvider.getColumnEntityName(entity.table, entity.table.name + "." + firstProperty(field.property)) ! "" />
import com.etendorx.entities.jparepo.${columnType}Repository;
</#if>
</#list>
import com.etendorx.entities.mapper.lib.JsonPathEntityRetrieverDefault;
import com.etendorx.entities.mapper.lib.ReturnKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.springframework.stereotype.Component;

import com.etendorx.entities.mapper.lib.JsonPathConverterBase;
import com.etendorx.entities.mapper.lib.JsonPathEntityRetriever;

@Component
@Slf4j
public class ${mappingPrefix}${entity.externalName}JsonPathConverter extends JsonPathConverterBase<${mappingPrefix}${entity.externalName}DTOWrite> {

  private final MappingUtils mappingUtils;
  <#list objectFields as field>
  <#if field.property??>
    <#assign columnType = modelProvider.getColumnTypeFullQualified(entity.table, entity.table.name + "." + firstProperty(field.property)) ! "" />
    <#if columnType?? && columnType != "">
  private final JsonPathEntityRetriever<${columnType}> ${field.name}Retriever;
    <#else>
      <#if field.fieldMapping == "EM" && genUtils.isOneToMany(field)>
    private final ${genUtils.getJsonPathConverter(field)} ${field.name}JsonPathConverter;
      </#if>
    </#if>
  </#if>
  </#list>

  public ${mappingPrefix}${entity.externalName}JsonPathConverter(
    MappingUtils mappingUtils<#if objectFields?size gt 0>,</#if>
<#list objectFields as field>
  // field ${field.name}
  <#if field.property??>
    <#assign columnType = modelProvider.getColumnTypeFullQualified(entity.table, entity.table.name + "." + firstProperty(field.property)) ! "" />
    <#if field.etrxProjectionEntityRelated??>
      <#assign targetEntityName = field.etrxProjectionEntityRelated.externalName />
    <#else>
      <#assign targetEntityName = modelProvider.getColumnEntityName(entity.table, entity.table.name + "." + firstProperty(field.property)) ! "" />
    </#if>
    // columnType ${columnType}
    <#if columnType?? && columnType != "">
      <#if field.fieldMapping == "DM">
        <#assign targetEntityName = modelProvider.getColumnEntityName(entity.table, entity.table.name + "." + firstProperty(field.property)) ! "" />
    ${targetEntityName}Repository ${field.name}Repository<#if field_has_next>,</#if>
      <#else>
    ${mappingPrefix}${targetEntityName}JsonPathRetriever ${field.name}Retriever<#if field_has_next>,</#if>
      </#if>
    <#else>
        <#if field.fieldMapping == "EM" && genUtils.isOneToMany(field)>
    ${genUtils.getJsonPathConverter(field)} ${field.name}JsonPathConverter<#if field_has_next>,</#if>
        </#if>
    </#if>
  </#if>
</#list>
  ) {
    super();
    this.mappingUtils = mappingUtils;
  <#list objectFields as field>
    <#if field.fieldMapping == "DM">
      <#assign targetEntityName = modelProvider.getColumnEntityName(entity.table, entity.table.name + "." + firstProperty(field.property)) ! "" />
    this.${field.name}Retriever = new JsonPathEntityRetrieverDefault<>(${field.name}Repository);
    <#else>
      <#if field.fieldMapping == "EM" && genUtils.isOneToMany(field)>
    this.${field.name}JsonPathConverter = ${field.name}JsonPathConverter;
      <#else>
    this.${field.name}Retriever = ${field.name}Retriever;
      </#if>
    </#if>
  </#list>
  }

  @Override
  public ${mappingPrefix}${entity.externalName}DTOWrite convert(String rawData) {
    var ctx = getReadContext(rawData);
    List<ReturnKey<?>> values = new ArrayList<>();

    ${mappingPrefix}${entity.externalName}DTOWrite dto = new ${mappingPrefix}${entity.externalName}DTOWrite();
  <#list entity.fields as field>
  <#assign hasRetriever = false />
  <#if field.property??>
    <#assign columnType = modelProvider.getColumnTypeFullQualified(entity.table, entity.table.name + "." + firstProperty(field.property)) ! "" />
    <#if columnType?? && columnType != "">
      <#assign hasRetriever = true />
    </#if>
  </#if>
  <#assign returnClass = ""/>
    log.debug("-- Parsing {}", "${field.name}");
  <#if field.fieldMapping == "CM">
    dto.set<@toCamelCase field.name />(mappingUtils.constantValue("${field.constantValue.id}"));
  <#else>
    <#if hasRetriever>
      <#if field.constantValue??>
    var _${NamingUtil.getSafeJavaName(field.name)} = retrieve${field.name?cap_first}(
      mappingUtils.constantValue("${field.constantValue.id}")
    );
      <#else>
    var _${NamingUtil.getSafeJavaName(field.name)} = read(ctx, "${field.jsonPath!"$."+field.name}", String.class);
      </#if>
    <#elseif field.property??>
      <#assign returnClass = modelProvider.getColumnPrimitiveType(entity.table, entity.table.name + "." + firstProperty(field.property)) ! "" />
    var _${NamingUtil.getSafeJavaName(field.name)} = read(ctx, "${field.jsonPath!"$."+field.name}"<#if returnClass != "">, <#if returnClass == "java.util.Date">String<#else>${returnClass}</#if>.class<#else>, Object.class</#if>);
    <#else>
    var _${NamingUtil.getSafeJavaName(field.name)} = read(ctx, "${field.jsonPath!"$."+field.name}", <#if genUtils.isOneToMany(field)>List<#else>Object</#if>.class);
    </#if>
    values.add(_${NamingUtil.getSafeJavaName(field.name)});
    log.debug("pathConverter ${entity.externalName} \"${field.jsonPath!"$."+field.name}\": {}", _${NamingUtil.getSafeJavaName(field.name)});
    if(!_${NamingUtil.getSafeJavaName(field.name)}.isNullValue()) {
      <#if hasRetriever>
      var val_${NamingUtil.getSafeJavaName(field.name)} = this.retrieve${field.name?cap_first}(_${NamingUtil.getSafeJavaName(field.name)}.getValue());
      dto.set<@toCamelCase field.name />(
        val_${NamingUtil.getSafeJavaName(field.name)}
      );
      <#else>
        <#if field.fieldMapping == "EM" && genUtils.isOneToMany(field) >
      ObjectMapper objectMapper = new ObjectMapper();
      List<${genUtils.getDto(field, "W")}> list = new ArrayList<>();
      for (Object o : (JSONArray) _${NamingUtil.getSafeJavaName(field.name)}.getValue()) {
        try {
          list.add(${field.name}JsonPathConverter.convert(objectMapper.writeValueAsString(o)) );
        } catch (JsonProcessingException e) {
          throw new IllegalArgumentException(e);
        }
      }
        </#if>
      dto.set<@toCamelCase field.name />(
    <#if returnClass=="java.util.Date">
        mappingUtils.parseDate(_${NamingUtil.getSafeJavaName(field.name)}.getValue())
    <#else>
      <#if field.fieldMapping == "EM" && genUtils.isOneToMany(field) >
        list
      <#else>
        _${NamingUtil.getSafeJavaName(field.name)}.getValue()
      </#if>
    </#if>
      );
      </#if>
    }
  </#if>
    log.debug("// End Parsing {}", "${field.name}");
  </#list>
    validateValues(values);
    return dto;
  }

<#list objectFields as field>
  <#if field.fieldMapping == "EM" && genUtils.isOneToMany(field)>
  <#else>
  <#assign columnType = modelProvider.getColumnTypeFullQualified(entity.table, entity.table.name + "." + firstProperty(field.property)) ! "" />
  private ${columnType} retrieve${field.name?cap_first}(Object id) {
  <#if field.fieldMapping == "DM">
    return ${field.name}Retriever.get("${secondProperty(field.property)}", (String) id);
  <#else>
    return ${field.name}Retriever.get(id);
  </#if>
  }
  </#if>
</#list>

}
