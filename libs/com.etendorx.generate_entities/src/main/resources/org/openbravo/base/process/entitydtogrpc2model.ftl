<#-- @formatter:off -->
<#function is_object string>
    <#if string == "java.lang.String" || string == "String" || string == "java.math.BigDecimal" || string == "java.lang.Long" || string == "java.util.Date" || string == "java.lang.Boolean"><#return false><#else><#return true></#if>
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

package com.etendorx.integration.mobilesync.dto;

import com.etendorx.das.grpc.common.${name};
import ${packageName}.${name}${projectionName?cap_first}Model;

import java.math.BigDecimal;
import java.util.Date;

public class ${name}DTOGrpc2${projectionName?cap_first} {
    public static ${name}${projectionName?cap_first}Model apply(${name} u) {
        var p = new ${name}${projectionName?cap_first}Model();
        <#list fields as field>
        <#if is_object(field.type)>
        p.set${field.name?cap_first}Id(u.get${field.name?cap_first}Id());
        <#elseif field.type == "java.util.Date">
        p.set${field.name?cap_first}(new Date(u.get${field.name?cap_first}().getSeconds() * 1000));
        <#elseif field.type == "java.math.BigDecimal">
        p.set${field.name?cap_first}(BigDecimal.valueOf(u.get${field.name?cap_first}()));
        <#else>
        p.set${field.name?cap_first}(u.get${field.name?cap_first}());
        </#if>

        </#list>
        return p;
    }
}
