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

import java.util.HashMap;
import java.util.Date;

public class ${name}DTO {
    public static HashMap<String, Object> apply(${name} u) {
        var p = new HashMap<String, Object>();
<#list fields as field>
    <#if is_object(field.type)>
        p.put("${field.name?lower_case}_id", u.get${field.name?cap_first}Id());
    <#elseif field.type == "java.util.Date">
        p.put("${field.name?lower_case}", new Date(u.get${field.name?cap_first}().getSeconds()));
    <#else>
        p.put("${field.name?lower_case}", u.get${field.name?cap_first}());
    </#if>
</#list>
        return p;
    }
}
