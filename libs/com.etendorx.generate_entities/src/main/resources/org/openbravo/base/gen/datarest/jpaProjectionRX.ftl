/**
 * Copyright 2022 Futit Services SL
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
package ${entity.getPackageName()};

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ${projectionName?cap_first} ${newClassName} Projection Class
 *
 * @author Sebastian Barrozo
 */
@Projection(name = "${projectionName}", types = ${entity.getPackageName()}.${entity.simpleClassName}.class)
public interface ${entity.simpleClassName}${projectionName?cap_first}Projection {
<#list entity.properties as p>
    <#assign showField = false><#assign idDbName = ""><#if projectionFields?size == 0><#assign showField = true>
    <#else>
        <#list projectionFields as field>
            <#if field.name == p.javaName && (computedColumns || !p.computedColumn)><#assign showField = true></#if>
        </#list>
    </#if>
    <#if showField>
        <#if !p.computedColumn>
            <#if p.isId()>
    @JsonProperty("${p.javaName}")
    java.lang.String get${p.javaName?cap_first}();

            </#if>
            <#if p.isPrimitive() && !p.isId()>
                    <#if !p.getPrimitiveType().isArray()>
    @JsonProperty("${p.javaName}")
    ${p.getObjectTypeName()} get${p.javaName?cap_first}();

                    <#else>
    @JsonProperty("${p.javaName}")
    String get${p.javaName?cap_first}();

                </#if>
            <#else>
                <#if p.targetEntity?? && !p.isOneToMany() && !p.isId() && !p.getTargetEntity().isView()>
                    <#if p.targetEntity?? >
    @Value("${'#'}{target.get${p.javaName?cap_first}() != null ? target.get${p.javaName?cap_first}().getId() : null }")
    @JsonProperty("${p.javaName}Id")
    String get${p.javaName?cap_first}Id();

                    <#else>
                        <#if p.targetEntity?? && !p.isId() && !p.getTargetEntity().isView()>
                        </#if>
                    </#if>
                <#else>
                    <#if projectionName != "default">
                        <#if p.oneToMany?? && p.oneToMany && p.targetEntity?? && !p.getTargetEntity().isView() && !p.targetEntity.className?ends_with("_ComputedColumns")>
                            <#assign anotherProjection = {}>
                            <#list anotherEntities as anotherEntity>
                                <#if anotherEntity.table.tableName == p.targetEntity.tableName>
                                    <#assign anotherProjection = anotherEntity>
                                </#if>
                            </#list>
    @JsonProperty("${p.javaName}")
                            <#if anotherProjection?has_content>
    @Value("${'#'}{target.${p.javaName}.![new com.etendorx.entities.utilities.KeyValueMap(<#list anotherProjection.fields as field>'${field.name}', <#if field.property??>${field.property}<#else>${field.name}</#if><#if !field?is_last>, </#if></#list>)]}")
    java.util.Set<java.util.Map<String, Object>> get${p.javaName?cap_first}();
                            <#else>
    java.util.Set<${p.targetEntity.className}> get${p.javaName?cap_first}();
                            </#if>

                        </#if>
                    </#if>
                </#if>
            </#if>
        <#else>
            <#if !p.targetEntity?? >
    @JsonProperty("${p.javaName}")
    ${p.getObjectTypeName()} get${p.javaName?cap_first}();

            </#if>
        </#if>
    </#if>
</#list>
}
