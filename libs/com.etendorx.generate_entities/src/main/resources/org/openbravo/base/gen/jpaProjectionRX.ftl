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
    java.lang.String get${p.javaName?cap_first}();

    </#if>
    <#if p.isPrimitive() && !p.isId()>
    <#if !p.getPrimitiveType().isArray()>
    ${p.getObjectTypeName()} get${p.javaName?cap_first}();

    <#else>
    String get${p.javaName?cap_first}();

    </#if>
    <#else>
    <#if p.targetEntity?? && !p.isOneToMany() && !p.isId() && !p.getTargetEntity().isView()>
    <#if p.targetEntity?? >
    @Value("${'#'}{target.get${p.javaName?cap_first}() != null ? target.get${p.javaName?cap_first}().getId() : null }")
    String get${p.javaName?cap_first}Id();

    <#else>
    <#if p.targetEntity?? && !p.isId() && !p.getTargetEntity().isView()>
    </#if>
    </#if>
    </#if>
    </#if>
    <#else>
    <#if !p.targetEntity?? >
    ${p.getObjectTypeName()} get${p.javaName?cap_first}();

    </#if>
    </#if>
    </#if>
</#list>
<#list projectionFields as field>
    <#if field.value??>
    <#if field.notNullValue??>
    @Value("${'#'}{${field.notNullValue?replace('#TARGET#', 'target')} ? target${field.value?replace('#TARGET#', 'target')} : null}")
    <#else>
    @Value("${'#'}{${field.value?replace('#TARGET#', 'target')}}")
    </#if>
    <#if field.type??>${field.type}<#else>String</#if> get${field.name?cap_first}();

    <#elseif field.projectedEntity??>
    <#if field.projectedEntity == "target">
    @Value("${'#'}{target.get${field.projectedField?cap_first}()}")
    <#else>
    @Value("${'#'}{target.get${field.projectedEntity?cap_first}() != null ? target.get${field.projectedEntity?cap_first}().get${field.projectedField?cap_first}() : null }")
    </#if>
    <#if field.type??>${field.type}<#else>String</#if> get${field.name?cap_first}();

    </#if>
</#list>
}
