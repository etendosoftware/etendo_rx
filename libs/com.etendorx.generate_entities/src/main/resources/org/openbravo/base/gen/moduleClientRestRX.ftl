<#include "pluralize.ftl">
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
package com.etendorx.entities.jparepo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;
import com.etendorx.clientrest.base.ClientRestBase;
import com.etendorx.clientrest.base.FeignConfiguration;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.hateoas.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * ${projectionName?cap_first} ${newClassName} Projection Class
 *
 */
@FeignClient(name = "${newClassName}${projectionName?cap_first}", url = "${"$"}{das.url}/cBpartners", configuration = FeignConfiguration.class)
@Projection(name = "${projectionName?pluralize}", types = ${packageName}.${entity.getPackageName()}.${newClassName}.class)
public interface ${newClassName}ClientRest {
<#list entity.properties as p><#compress>
    <#assign showField = false><#assign idDbName = ""><#if projectionFields?size == 0><#assign showField = true>
    <#else>
    <#list projectionFields as field>
    <#if field.name == p.javaName && (computedColumns || !p.computedColumn)><#assign showField = true></#if>
    </#list>
    </#if></#compress>

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
    @Value("${field.value}")
    ${field.type} get${field.name?cap_first}();

    </#if>
</#list>
}
