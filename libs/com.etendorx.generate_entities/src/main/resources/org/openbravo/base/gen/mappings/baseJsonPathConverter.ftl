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
    <#assign columnType = genUtils.getFullQualifiedType(entity, field) ! "" />
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
<#assign columnType = modelProvider.getColumnEntityName(entity.table, entity.table.name + "." + genUtils.firstProperty(field.property)) ! "" />
import com.etendorx.entities.jparepo.${columnType}Repository;
</#if>
</#list>
import com.etendorx.entities.mapper.lib.ExternalIdService;
import com.etendorx.entities.mapper.lib.JsonPathConverterBase;
import com.etendorx.entities.mapper.lib.JsonPathEntityRetriever;
import com.etendorx.entities.mapper.lib.JsonPathEntityRetrieverDefault;
import com.etendorx.entities.mapper.lib.ReturnKey;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.etendorx.entities.mapper.lib.JsonPathConverterBase;
import com.etendorx.entities.mapper.lib.JsonPathEntityRetriever;

@Component
@Slf4j
public class ${mappingPrefix}${entity.externalName}JsonPathConverter extends JsonPathConverterBase<${mappingPrefix}${entity.externalName}DTOWrite> {

  private final MappingUtils mappingUtils;
  private final ExternalIdService externalIdService;

  <#list objectFields as field>
  <#if field.property??>
    <#if field.fieldMapping == "EM" && field.createRelated?? && field.createRelated>
  private final ${genUtils.getJsonPathConverter(field)} ${field.name}JsonPathConverter;
    <#elseif field.fieldMapping == "EM" && genUtils.isOneToMany(field) >
      <#if field.etrxProjectionEntityRelated??>
        <#assign targetEntityName = field.etrxProjectionEntityRelated.externalName />
      <#else>
        <#assign targetEntityName = modelProvider.getColumnEntityName(entity.table, entity.table.name + "." + genUtils.firstProperty(field.property)) ! "" />
      </#if>
  private final ${mappingPrefix}${targetEntityName}JsonPathRetriever ${field.name}Retriever;
    <#else>
      <#assign columnType = genUtils.getFullQualifiedType(entity, field) ! "" />
      <#if columnType?? && columnType != "">
  private final JsonPathEntityRetriever<${columnType}> ${field.name}Retriever;
      </#if>
    </#if>
  </#if>
  </#list>

  public ${mappingPrefix}${entity.externalName}JsonPathConverter(
    MappingUtils mappingUtils,
    ExternalIdService externalIdService<#if objectFields?size gt 0>,</#if>
<#list objectFields as field>
    <#assign columnType = genUtils.getFullQualifiedType(entity, field) ! "" />
    <#if field.etrxProjectionEntityRelated??>
      <#assign targetEntityName = field.etrxProjectionEntityRelated.externalName />
    <#else>
      <#assign targetEntityName = modelProvider.getColumnEntityName(entity.table, entity.table.name + "." + genUtils.firstProperty(field.property)) ! "" />
    </#if>
    <#if field.fieldMapping == "EM" && field.createRelated?? && field.createRelated>
      ${genUtils.getJsonPathConverter(field)} ${field.name}JsonPathConverter<#if field_has_next>,</#if>
    <#elseif field.fieldMapping == "EM" && genUtils.isOneToMany(field)>
      ${mappingPrefix}${targetEntityName}JsonPathRetriever ${field.name}Retriever<#if field_has_next>,</#if>
    <#elseif field.fieldMapping == "DM">
      <#assign targetEntityName = modelProvider.getColumnEntityName(entity.table, entity.table.name + "." + genUtils.firstProperty(field.property)) ! "" />
    ${targetEntityName}Repository ${field.name}Repository<#if field_has_next>,</#if>
      <#else>
    ${mappingPrefix}${targetEntityName}JsonPathRetriever ${field.name}Retriever<#if field_has_next>,</#if>
    </#if>
</#list>
  ) {
    super();
    this.mappingUtils = mappingUtils;
    this.externalIdService = externalIdService;
  <#list objectFields as field>
    <#if field.fieldMapping == "EM" && field.createRelated?? && field.createRelated>
    this.${field.name}JsonPathConverter = ${field.name}JsonPathConverter;
    <#elseif field.fieldMapping == "DM">
      <#assign targetEntityName = modelProvider.getColumnEntityName(entity.table, entity.table.name + "." + genUtils.firstProperty(field.property)) ! "" />
    this.${field.name}Retriever = new JsonPathEntityRetrieverDefault<>(${field.name}Repository, externalIdService, "${genUtils.getPropertyTableId(field)}");
    <#else>
    this.${field.name}Retriever = ${field.name}Retriever;
    </#if>
  </#list>
  }

  @Override
  public ${mappingPrefix}${entity.externalName}DTOWrite convert(String rawData) {
    var ctx = getReadContext(rawData);
    List<ReturnKey<?>> values = new ArrayList<>();
    ObjectMapper objectMapper = new ObjectMapper();

    ${mappingPrefix}${entity.externalName}DTOWrite dto = new ${mappingPrefix}${entity.externalName}DTOWrite();
  <#list entity.fields as field>
  <#assign hasRetriever = false />
  <#if field.property??>
    <#assign columnType = genUtils.getFullQualifiedType(entity, field) ! "" />
    <#if columnType?? && columnType != "">
      <#assign hasRetriever = true />
    </#if>
  </#if>
  <#assign returnClass = ""/>
    log.debug("-- Parsing {}", "${field.name}");
  <#if field.fieldMapping == "CM">
    <#assign returnClass = genUtils.getPrimitiveType(entity, field) ! "" />
    <#if returnClass == "java.math.BigDecimal">
    dto.set<@toCamelCase field.name />(NumberUtils.createBigDecimal( mappingUtils.constantValue("${field.constantValue.id}")));
    <#elseif returnClass == "java.lang.Boolean">
    dto.set<@toCamelCase field.name />(StringUtils.equals(mappingUtils.constantValue("${field.constantValue.id}"), "Y"));
    <#else>
    dto.set<@toCamelCase field.name />(mappingUtils.constantValue("${field.constantValue.id}"));
    </#if>
  <#else>
    <#if field.jsonPath??>
      <#assign jsonPath = field.jsonPath?replace("\"", "\\\"") />
    <#else>
      <#assign jsonPath = "$." + field.name />
    </#if>
    <#if hasRetriever>
      <#if field.fieldMapping == "CM" && field.constantValue??>
    var _${NamingUtil.getSafeJavaName(field.name)} = retrieve${field.name?cap_first}(
      mappingUtils.constantValue("${field.constantValue.id}")
    );
      <#else>
        <#assign returnClass = genUtils.getPrimitiveType(entity, field) ! "" />
    var _${NamingUtil.getSafeJavaName(field.name)} = read(ctx, "${jsonPath!"$."+field.name}"<#if returnClass != "">, <#if returnClass == "java.util.Date">String<#else>${returnClass}</#if>.class<#else>, Object.class</#if>, <#if field.constantValue??> mappingUtils.constantValue("${field.constantValue.id}")<#else>null</#if>);
      </#if>
    <#elseif field.property??>
      <#assign returnClass = genUtils.getPrimitiveType(entity, field) ! "" />
    var _${NamingUtil.getSafeJavaName(field.name)} = read(ctx, "${jsonPath!"$."+field.name}"<#if returnClass != "">, <#if returnClass == "java.util.Date">String<#else>${returnClass}</#if>.class<#else>, Object.class</#if>, <#if field.constantValue??> mappingUtils.constantValue("${field.constantValue.id}")<#else>null</#if>);
    <#else>
    var _${NamingUtil.getSafeJavaName(field.name)} = read(ctx, "${jsonPath!"$."+field.name}", <#if genUtils.isOneToMany(field)>List<#else>Object</#if>.class, <#if field.constantValue??> mappingUtils.constantValue("${field.constantValue.id}")<#else>null</#if>);
    </#if>
    values.add(_${NamingUtil.getSafeJavaName(field.name)});
    log.debug("pathConverter ${entity.externalName} \"${jsonPath!"$."+field.name}\": {}", _${NamingUtil.getSafeJavaName(field.name)});
    if(!_${NamingUtil.getSafeJavaName(field.name)}.isNullValue()) {
      <#if hasRetriever>
        <#if field.fieldMapping == "EM" && field.createRelated?? && field.createRelated>
      try {
        dto.set${field.name?cap_first}(
          ${field.name}JsonPathConverter.convert(objectMapper.writeValueAsString(_${NamingUtil.getSafeJavaName(field.name)}.getValue()))
        );
      } catch (Exception e) {
        throw new IllegalArgumentException(e);
      }
        <#else>
      var val_${NamingUtil.getSafeJavaName(field.name)} = this.retrieve${field.name?cap_first}(_${NamingUtil.getSafeJavaName(field.name)}.getValue());
      dto.set<@toCamelCase field.name />(
        val_${NamingUtil.getSafeJavaName(field.name)}
      );
        </#if>
      <#else>
        <#if field.fieldMapping == "EM" && genUtils.isOneToMany(field) >
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
  <#if field.fieldMapping == "EM" && (genUtils.isOneToMany(field) || (field.createRelated?? && field.createRelated)) >
  <#else>
  <#assign columnType = genUtils.getFullQualifiedType(entity, field) ! "" />
  private ${columnType} retrieve${field.name?cap_first}(Object id) {
  <#if field.fieldMapping == "DM">
    return ${field.name}Retriever.get("${secondProperty(field.property)}", toString(id));
  <#elseif field.fieldMapping == "CM">
    return ${field.name}Retriever.get("${secondProperty(field.property)}", toString(id));
  <#else>
    return ${field.name}Retriever.get(id);
  </#if>
  }
  </#if>
</#list>

}
