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

import static com.etendorx.integration.mobilesync.service.GEtendoSync.getTime;

public class ${name}DTO${projectionName?cap_first}2Grpc {
  public static ${name} apply(${name}${projectionName?cap_first}Model u) {
    var p = ${name}.newBuilder();
<#list fields as field>
    <#if is_object(field.type)>
    if (u.get${field.name?cap_first}Id() != null) {
      p.set${field.name?cap_first}Id(u.get${field.name?cap_first}Id());
    }
    <#elseif field.type == "java.util.Date">
    if (u.get${field.name?cap_first}() != null) {
      p.set${field.name?cap_first}(getTime(u.get${field.name?cap_first}()));
    }
    <#elseif field.type == "java.math.BigDecimal">
    if (u.get${field.name?cap_first}() != null) {
      p.set${field.name?cap_first}(u.get${field.name?cap_first}().doubleValue());
    }
    <#else>
    if (u.get${field.name?cap_first}() != null) {
      p.set${field.name?cap_first}(u.get${field.name?cap_first}());
    }
    </#if>
</#list>
    return p.build();
  }
}
