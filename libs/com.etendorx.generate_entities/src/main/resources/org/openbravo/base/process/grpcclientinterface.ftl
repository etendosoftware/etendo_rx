<#-- @formatter:off -->
<#function is_object string>
    <#if string == "java.lang.String" || string == "String" || string == "java.math.BigDecimal" || string == "java.lang.Long" || string == "java.util.Date" || string == "java.lang.Boolean"><#return false><#else><#return true></#if>
</#function>
<#function cast_par p>
    -- ${p.type} --
    <#if p.type == "java.util.Date">new Date(request.get${p.name?cap_first}().getSeconds())<#else>request.get${p.name?cap_first}()</#if>
</#function>
/*
* Copyright 2022  Futit Services SL
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.etendorx.integration.mobilesync.service;

import com.etendorx.das.grpc.common.${name}GrpcRepositoryGrpc;
<#list searches as search>
import com.etendorx.das.grpc.common.${name}_${search.method}Search;
</#list>
import com.etendorx.integration.mobilesync.dto.${name}DTOGrpc2${projectionName?cap_first};
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.etendorx.integration.mobilesync.service.GEtendoSync.getTime;

public interface ${name}${projectionName?cap_first}DasService {

<#list searches as search>
    public List<${packageName}.${name}${projectionName?cap_first}Model> ${search.method}(
        <#list search.params as p><#if p.type?? && p.type == "java.util.Date">Date ${p.name}<#else>${p.type} ${p.name}</#if><#if p?has_next>, </#if></#list>
    );

</#list>
<#if transactional>
    ${packageName}.${name}${projectionName?cap_first}Model save(
    ${packageName}.${name}${projectionName?cap_first}Model entity
    );
</#if>
}