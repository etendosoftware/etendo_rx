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
<#function firstProperty property>
  <#list property?split(".") as part>
    <#return part>
  </#list>
</#function>
<#macro convertToGetMethod path>
  <#compress>
  <#assign result="entity">
  <#list path?split(".") as part>
    <#assign result = result + ".get" + part?cap_first + "()">
  </#list>
  ${result}
  </#compress>
</#macro>
<#function firstProperty property>
  <#list property?split(".") as part>
    <#return part>
  </#list>
</#function>
<#assign extMappings = []>
<#list entity.fields as field>
  <#if field.fieldMapping == "JM" || field.fieldMapping == "EM">
    <#assign extMappings = extMappings + [field]>
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

<#list extMappings as field>
  <#if field.fieldMapping == "EM" && field.createRelated?? && field.createRelated>
import com.etendorx.entities.jparepo.${genUtils.getRepository(field)};
  </#if>
</#list>
import com.etendorx.entities.mapper.lib.DTOWriteMapping;
import com.etendorx.entities.mapper.lib.ExternalIdService;
import ${entity.table.thePackage.javaPackage}.${entity.table.className};
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.etendorx.entities.entities.AuditServiceInterceptor;

import java.util.ArrayList;

@Component
public class ${mappingPrefix}${entity.externalName}FieldConverterWrite {

  private final AuditServiceInterceptor auditServiceInterceptor;
<#list extMappings as field>
  <#if field.fieldMapping == "JM">
  private final DTOWriteMapping<${entity.table.className}, ${mappingPrefix}${entity.externalName}DTOWrite> ${field.name};
  <#else>
  private final ${genUtils.getRetriever(field)} ${field.name}Retriever;
    <#if genUtils.isOneToMany(field) || (field.createRelated?? && field.createRelated)>
  private final ${genUtils.getDTOConverter(field)} ${field.name}Converter;
    </#if>
    <#if !genUtils.isOneToMany(field) && (field.createRelated?? && field.createRelated)>
  private final ${genUtils.getRepository(field)} ${field.name}Repository;
    </#if>
  </#if>
</#list>
  private final ExternalIdService externalIdService;

  public ${mappingPrefix}${entity.externalName}FieldConverterWrite(
<#list extMappings as field>
  <#if field.fieldMapping == "JM">
    @Qualifier("${field.javaMapping.qualifier}") @Autowired DTOWriteMapping<${entity.table.className}, ${mappingPrefix}${entity.externalName}DTOWrite> ${field.name},
  <#else>
    ${genUtils.getRetriever(field)} ${field.name}Retriever,
    <#if genUtils.isOneToMany(field) || (field.createRelated?? && field.createRelated)>
    ${genUtils.getDTOConverter(field)} ${field.name}Converter,
    </#if>
    <#if !genUtils.isOneToMany(field) && (field.createRelated?? && field.createRelated)>
    ${genUtils.getRepository(field)} ${field.name}Repository,
    </#if>
  </#if>
</#list>
    AuditServiceInterceptor auditServiceInterceptor,
    ExternalIdService externalIdService
  ) {
    super();
<#list extMappings as field>
  <#if field.fieldMapping == "JM">
    this.${field.name} = ${field.name};
  <#else>
    this.${field.name}Retriever = ${field.name}Retriever;
    <#if genUtils.isOneToMany(field) || (field.createRelated?? && field.createRelated)>
    this.${field.name}Converter = ${field.name}Converter;
    </#if>
    <#if !genUtils.isOneToMany(field) && (field.createRelated?? && field.createRelated)>
    this.${field.name}Repository = ${field.name}Repository;
    </#if>
  </#if>
</#list>
    this.auditServiceInterceptor = auditServiceInterceptor;
    this.externalIdService = externalIdService;
  }

  public String getId(${mappingPrefix}${entity.externalName}DTOWrite entity) {
    return entity.getId();
  }

<#list extMappings as field>
      <#if field.createRelated?? && field.createRelated>
      // createRelated ${field.createRelated?c}
      </#if>
  public void set<@toCamelCase field.name/>(${entity.table.className} entity, ${mappingPrefix}${entity.externalName}DTOWrite dto) {
    <#if field.fieldMapping == "JM">
    ${field.name}.map(entity, dto);
    <#else>
     <#if genUtils.isOneToMany(field)>
      if(entity.get${NamingUtil.getSafeJavaName(firstProperty(field.property))?cap_first}() == null) {
        entity.set${NamingUtil.getSafeJavaName(firstProperty(field.property))?cap_first}(new ArrayList<>());
      }
      if(dto.get${NamingUtil.getSafeJavaName(firstProperty(field.property))?cap_first}() == null) {
        return;
      }
      for (${genUtils.getDto(field, "")} el : dto.get${NamingUtil.getSafeJavaName(firstProperty(field.property))?cap_first}()) {
        ${genUtils.getReturnType(field)} line = this.${field.name}Retriever.get(el.getId());
        if(line == null) {
          line = new ${genUtils.getReturnType(field)}();
        }
        <#if field.entityFieldMap??>
          <#list field.entityFieldMap as relField>
            <#if relField.property == "_identifier">
        line.set${NamingUtil.getSafeJavaName(firstProperty(relField.relatedField.property))?cap_first}(entity);
            <#else>
        line.set${NamingUtil.getSafeJavaName(firstProperty(relField.relatedField.property))?cap_first}(entity.get${NamingUtil.getSafeJavaName(firstProperty(relField.property))?cap_first}());
            </#if>
          </#list>
        </#if>
        line = ${field.name}Converter.convert(el, line);
        <#if field.entityFieldMap??>
          <#list field.entityFieldMap as relField>
            <#if relField.property == "_identifier">
        line.set${NamingUtil.getSafeJavaName(firstProperty(relField.relatedField.property))?cap_first}(entity);
            <#else>
        line.set${NamingUtil.getSafeJavaName(firstProperty(relField.relatedField.property))?cap_first}(entity.get${NamingUtil.getSafeJavaName(firstProperty(relField.property))?cap_first}());
            </#if>
          </#list>
        </#if>
        auditServiceInterceptor.setAuditValues(line, true);
        entity.get${NamingUtil.getSafeJavaName(firstProperty(field.property))?cap_first}().add(line);
        externalIdService.add("${genUtils.getPropertyTableId(field)}", el.getId(), line);
      }
      <#else>
      var dtoRel = dto.get${NamingUtil.getSafeJavaName(firstProperty(field.property))?cap_first}();
      <#assign typeVar=genUtils.getReturnType(field)>
      <#if typeVar == "Object">
        <#assign typeVar="var">
      </#if>
      ${typeVar} el = dtoRel == null ? null : this.${field.name}Retriever.get(dtoRel.getId());
        <#if field.createRelated?? && field.createRelated>
      if(el == null) {
        el = new ${genUtils.getReturnType(field)}();
      }
      <#if field.entityFieldMap??>
        <#list field.entityFieldMap as relField>
          <#if relField.property == "_identifier">
      el.set${NamingUtil.getSafeJavaName(firstProperty(relField.relatedField.property))?cap_first}(entity);
          <#else>
      el.set${NamingUtil.getSafeJavaName(firstProperty(relField.relatedField.property))?cap_first}(entity.get${NamingUtil.getSafeJavaName(firstProperty(relField.property))?cap_first}());
          </#if>
        </#list>
      </#if>
      el = ${field.name}Converter.convert(dtoRel, el);
      <#if field.entityFieldMap??>
        <#list field.entityFieldMap as relField>
          <#if relField.property == "_identifier">
      el.set${NamingUtil.getSafeJavaName(firstProperty(relField.relatedField.property))?cap_first}(entity);
          <#else>
      el.set${NamingUtil.getSafeJavaName(firstProperty(relField.relatedField.property))?cap_first}(entity.get${NamingUtil.getSafeJavaName(firstProperty(relField.property))?cap_first}());
          </#if>
        </#list>
      </#if>
      auditServiceInterceptor.setAuditValues(el, true);
      ${field.name}Repository.save(el);
      externalIdService.add("${genUtils.getPropertyTableId(field)}", el.getId(), el);
        </#if>
      entity.set${NamingUtil.getSafeJavaName(firstProperty(field.property))?cap_first}(el);
      </#if>
    </#if>
  }

</#list>
<#list entity.fields as field>
  <#if (field.fieldMapping == "DM" || field.fieldMapping == "CM")>
  public void set<@toCamelCase field.name/>(${entity.table.className} entity, ${mappingPrefix}${entity.externalName}DTOWrite dto) {
    <#if field.property??>
      <#if field.property != "id">
    entity.set${NamingUtil.getSafeJavaName(firstProperty(field.property))?cap_first}(dto.get<@toCamelCase field.name/>());
      <#else>
    // Id property is not directly assignable in writer
      </#if>
    <#else>
    entity.set${field.name?cap_first}(dto.get<@toCamelCase field.name/>());
    </#if>
  }
  </#if>

</#list>
}
