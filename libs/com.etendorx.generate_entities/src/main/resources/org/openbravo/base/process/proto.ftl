<#-- @formatter:off -->
<#function is_object string>
 <#if string == "java.lang.String" || string == "String" || string == "java.math.BigDecimal" || string == "java.lang.Long" || string == "java.util.Date" || string == "java.lang.Boolean"><#return false><#else><#return true></#if>
</#function>
<#function proto_type string>
 <#switch string>
  <#case "java.lang.String"><#return "string"><#break>
  <#case "String"><#return "string"><#break>
  <#case "java.math.BigDecimal"><#return "double"><#break>
  <#case "java.lang.Long"><#return "int64"><#break>
  <#case "java.util.Date"><#return "google.protobuf.Timestamp"><#break>
  <#case "java.lang.Boolean"><#return "bool"><#break>
  <#default><#return string>
 </#switch>
</#function>
syntax = "proto3";
import "google/protobuf/timestamp.proto";

option java_multiple_files = true;
package com.etendorx.das.grpc.common;

<#list entities as entity>
message ${entity.name} {
<#list entity.fields as field>
<#if is_object(field.type)>
string ${field.name}Id = ${field?index + 1};
<#else>
${proto_type(field.type)} ${field.name} = ${field?index + 1};
</#if>
</#list>
}

message ${entity.name}List {
int32 size = 1;
repeated ${entity.name} ${entity.name?lower_case} = 2;
}

</#list>

<#list repositories as repository>
/*
 * ${repository.name} Repository
 */
<#list repository.searches as search>
message ${repository.name}_${search.method}Search {
<#list search.params as param>
${proto_type(param.type)} ${param.name} = ${param?index + 1};
</#list>
}
</#list>
<#if repository.transactional>
    message ${repository.name}_saveParam {
    ${repository.name} entity = 1;
    }
</#if>
service ${repository.name}GrpcRepository {
<#list repository.searches as search>
rpc ${search.method}(${repository.name}_${search.method}Search) returns (${repository.name}List);
</#list>
    <#if repository.transactional>
        rpc save(${repository.name}_saveParam) returns (${repository.name});
    </#if>
}
</#list>
