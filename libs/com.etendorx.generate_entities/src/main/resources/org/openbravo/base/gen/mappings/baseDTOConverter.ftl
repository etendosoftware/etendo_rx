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

import com.etendorx.entities.mapper.lib.DTOConverterBase;
import ${readEntity.table.thePackage.javaPackage}.${readEntity.table.className};
import org.springframework.stereotype.Component;

@Component
public class ${mappingPrefix}${readEntity.externalName}DTOConverter extends
    DTOConverterBase<${readEntity.table.className}, ${mappingPrefix}${readEntity.externalName}DTORead, ${mappingPrefix}${readEntity.externalName}DTOWrite> {

  private final ${mappingPrefix}${readEntity.externalName}FieldConverterRead readConverter;
  <#if writeEntity??>
  private final ${mappingPrefix}${writeEntity.externalName}FieldConverterWrite writeConverter;
  </#if>

  public ${mappingPrefix}${readEntity.externalName}DTOConverter(
    ${mappingPrefix}${readEntity.externalName}FieldConverterRead readConverter<#if writeEntity??>, ${mappingPrefix}${writeEntity.externalName}FieldConverterWrite writeConverter</#if>) {
    this.readConverter = readConverter;
<#if writeEntity??>
    this.writeConverter = writeConverter;
</#if>
  }

  // READ
  @Override
  public ${mappingPrefix}${readEntity.externalName}DTORead convert(${readEntity.table.className} entity) {
    if (entity == null) {
      return null;
    }
    ${mappingPrefix}${readEntity.externalName}DTORead dto = new ${mappingPrefix}${readEntity.externalName}DTORead();
<#list readEntity.fields as field>
    dto.set<@toCamelCase field.name?trim?replace("\n", "", "r")/>(readConverter.get<@toCamelCase field.name?trim?replace("\n", "", "r")/>(entity));
</#list>
    return dto;
  }

  // WRITE
<#if writeEntity??>
  @Override
  public ${writeEntity.table.className} convert(${mappingPrefix}${writeEntity.externalName}DTOWrite dto, ${writeEntity.table.className} entity) {
    if (entity == null) {
      entity = new ${writeEntity.table.className}();
    }
<#list writeEntity.fields as field>
  <#assign isOneToMany = field.property?? && modelProvider.isColumnIsOneToMany(writeEntity.table, writeEntity.table.name + "." + field.property)>
  <#if !isOneToMany>
    writeConverter.set<@toCamelCase field.name?trim?replace("\n", "", "r")/>(entity, dto);
  </#if>
</#list>
    return entity;
  }

  @Override
  public ${writeEntity.table.className} convertOneToMany(${mappingPrefix}${writeEntity.externalName}DTOWrite dto, ${writeEntity.table.className}  entity) {
<#list writeEntity.fields as field>
  <#assign isOneToMany = field.property?? && modelProvider.isColumnIsOneToMany(writeEntity.table, writeEntity.table.name + "." + field.property)>
  <#if isOneToMany>
    writeConverter.set<@toCamelCase field.name?trim?replace("\n", "", "r")/>(entity, dto);
  </#if>
</#list>
    return entity;
  }
<#else>
  @Override
  public ${readEntity.table.className} convert((String) ${mappingPrefix}${readEntity.externalName}DTO dto, ${readEntity.table.className} entity) {
    return entity;
  }
</#if>

}
