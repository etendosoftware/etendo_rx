<#-- @formatter:off -->
package com.etendorx.entities.metadata;

import com.etendorx.entities.metadata.EntityMetadata;
import com.etendorx.entities.metadata.FieldMetadata;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ${entity.name}_Metadata_ extends EntityMetadata {
  public ${entity.name}_Metadata_() {
    setTableName("${entity.tableName?lower_case}");
    setEntityName("${entity.simpleClassName}");
    setAdTableId("${entity.tableId}");
<#list entity.properties as p>
    <#assign type = ""/>
    <#assign ad_table_id_rel = "null"/>
    <#assign columnName = ""/>
    <#assign columnId = ""/>
    <#assign isArray = "false"/>
    <#assign entityName = "null"/>
    <#if p.columnName??>
        <#assign columnName = p.columnName?lower_case/>
    </#if>
    <#if p.columnId??>
        <#assign columnId = p.columnId/>
    </#if>
    <#if !p.computedColumn>
        <#if p.isId()>
        <#assign type = "String"/>
        <#assign ad_table_id_rel = "\"${p.entity.tableId}\""/>
        <#assign entityName = "\"${p.entity.name}\""/>
        </#if>
        <#if p.columnName??>
            <#if p.isPrimitive() && !p.isId()>
                <#if p.javaName == "version">
        <#assign type = p.getObjectTypeName()/>
                <#else>
        <#assign type = p.shorterTypeName/>
                </#if>
            <#else>
                <#if p.targetEntity?? && !p.isOneToMany() && !p.isId()>
                    <#if p.targetEntity?? >
        <#assign type = p.targetEntity.className/>
        <#assign ad_table_id_rel = "\"${p.targetEntity.tableId}\""/>
        <#assign entityName = "\"${p.targetEntity.name}\""/>
                    <#else>
                    </#if>
                </#if>
            </#if>
        <#else>
            <#if p.oneToMany && p.targetEntity?? && !p.isId() && !p.targetEntity.className?ends_with("_ComputedColumns")>
        <#assign type = p.targetEntity.className/>
        <#assign ad_table_id_rel = "\"${p.targetEntity.tableId}\""/>
        <#assign entityName = "\"${p.targetEntity.name}\""/>
        <#assign isArray = "true"/>
            </#if>
        </#if>
    <#else>
        <#if computedColumns && p.computedColumn && !p.targetEntity??><#assign query>${p.sqlLogic}</#assign>
        <#assign type = p.getObjectTypeName()/>
        </#if>
    </#if>
    getFields().put("${p.javaName}", new FieldMetadata("${type}", "${columnName}", "${columnId}", ${ad_table_id_rel}, ${isArray}, ${entityName}));
</#list>
  }

}
