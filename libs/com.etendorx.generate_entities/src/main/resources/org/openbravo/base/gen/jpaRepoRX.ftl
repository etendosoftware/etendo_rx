<#function plural name1>
    <#if name1?ends_with('s') >
        <#return name1>
    <#elseif name1?ends_with('y') >
        <#return name1?substring(0, name1?length - 1) + 'ies'>
    <#else>
        <#return name1 + 's'>
    </#if>
</#function>
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
package ${packageJPARepo};

import com.etendorx.entities.entities.BaseDASRepository;
<#if dataRestEnabled == true>
import ${entity.getPackageName()}.${entity.simpleClassName}DefaultProjection;
</#if>
import ${entity.getPackageName()}.${entity.simpleClassName};
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
<#if dataRestEnabled == true>
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
</#if>
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * ${entity.simpleClassName} JPA CRUD Repository
 *
 */
<#if dataRestEnabled == true>
@RepositoryRestResource(excerptProjection = ${entity.simpleClassName}DefaultProjection.class, path = "${entity.name}")
</#if>
public interface ${entity.name}Repository extends BaseDASRepository<${entity.simpleClassName}>{
    <#if searches??>

    <#list searches as s>
    @Query(value = "${s.query?replace("^\\s+|\\s+$|\\n|\\r", " ", "rm")}")
    <#if (s.fetchAttributes??) && (s.fetchAttributes?size > 0)>
    @EntityGraph(value = "${newClassName}.detail", type = EntityGraph.EntityGraphType.LOAD, attributePaths = { <#list s.fetchAttributes as attr>"${attr}"<#if !attr?is_last>, </#if></#list> })
    </#if>
    Page<${entity.simpleClassName}> ${s.method}(
    <#list s.params as p>@Param("${p.name}") <#if p.type == 'java.util.Date'>@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)</#if> ${p.type} ${p.name},
    </#list>Pageable pageable);

    </#list>
    </#if>

}
