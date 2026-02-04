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
<#function getter p>
    <#if p.boolean>
        <#return "is" + p.getterSetterName?cap_first>
    <#else>
        <#return "get" + p.getterSetterName?cap_first>
    </#if>
</#function>
<#function plural name1>
    <#assign n = "">
    <#list name1?split("_") as sn>
        <#if sn?index == 1>
        <#assign n = n + sn?lower_case?cap_first>
        <#else>
        <#assign n = n + sn?lower_case>
        </#if>
    </#list>
    <#if n?ends_with('s') >
        <#return n>
    <#elseif n?ends_with('y') >
        <#return n?substring(0, n?length - 1) + 'ies'>
    <#else>
        <#return n + 's'>
    </#if>
</#function>
package ${packageEntityModelProjected};

import com.etendorx.clientrest.base.RepresentationWithId;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonGetter;
import org.springframework.hateoas.Link;

import java.net.URI;

@Getter
@Setter
public class ${newClassName}${projectionName?cap_first}Model extends RepresentationWithId<${newClassName}${projectionName?cap_first}Model> {
    <#list entity.properties as p>
    <#assign showField = false>
    <#if projectionFields?size == 0>
    <#assign showField = true>
    <#else>
    <#list projectionFields as field><#if field.name == p.javaName><#assign showField = true></#if></#list>
    </#if>
    <#if showField>
    <#if p.isId()>
    java.lang.String ${p.javaName};

    </#if>
    <#if p.isPrimitive() && !p.isId()>
    <#if !p.getPrimitiveType().isArray()>
    ${p.getObjectTypeName()} ${p.javaName};

    <#else>
    String ${p.javaName};

    </#if>
    <#else>
    <#if p.targetEntity?? && !p.isOneToMany() && !p.isId() && !p.getTargetEntity().isView()>
    <#if p.targetEntity?? >
    String ${p.javaName}Id;

    <#else>
    <#if p.targetEntity?? && !p.isId() && !p.getTargetEntity().isView()></#if>
    </#if>
    </#if>
    </#if>
    </#if>
    </#list>
    <#list projectionFields as field>
    <#if field.projectedEntity??>
    ${field.type} ${field.name};

    <#elseif field.value??>
    <#-- Only add if the field does not exist in entity.properties to avoid duplicates -->
    <#assign fieldExistsInEntity = false>
    <#list entity.properties as p>
        <#if p.javaName == field.name><#assign fieldExistsInEntity = true></#if>
    </#list>
    <#if !fieldExistsInEntity>
    ${field.type} ${field.name};

    </#if>
    </#if>
</#list>

    <#list entity.properties as p>
    <#assign showField = false>
    <#if projectionFields?size == 0>
    <#assign showField = true>
    <#else>
    <#list projectionFields as field><#if field.name == p.javaName><#assign showField = true></#if></#list>
    </#if>
    <#if showField>
    <#if p.targetEntity?? && !p.isOneToMany() && !p.isId() && !p.getTargetEntity().isView()>
    <#if p.targetEntity?? >
    @JsonGetter("${p.javaName}")
    URI ${p.javaName}() {
        return Link.of("/${p.targetEntity.name})/" + ${p.javaName}Id, "${p.javaName}").toUri();
    }

    </#if>
    </#if>
    </#if>
</#list>
}
