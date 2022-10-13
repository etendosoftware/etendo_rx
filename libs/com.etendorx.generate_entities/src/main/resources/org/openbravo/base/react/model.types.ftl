<#function is_object string>
 <#if string == "java.lang.String" || string == "String" || string == "java.math.BigDecimal" || string == "java.lang.Long" || string == "java.util.Date" || string == "java.lang.Boolean" || string == "java.sql.Timestamp"><#return false><#else><#return true></#if>
</#function>
<#function react_type string>
 <#switch string>
  <#case "java.lang.String"><#return "string"><#break>
  <#case "String"><#return "string"><#break>
  <#case "java.math.BigDecimal"><#return "double"><#break>
  <#case "java.lang.Long"><#return "int64"><#break>
  <#case "java.util.Date"><#return "google.protobuf.Timestamp"><#break>
  <#case "java.sql.Timestamp"><#return "google.protobuf.Timestamp"><#break>
  <#case "java.lang.Boolean"><#return "bool"><#break>
  <#default><#return string>
 </#switch>
</#function>
import { DASResponse, EntityType, KV } from "../base/baseservice.types";

<#if searches??>

<#list searches as s>
export type ${s.method?cap_first}Params = KV & {
  <#list s.params as p>${p.name}: ${react_type(p.type)}</#list>
}
</#list>
</#if>

export type ${entity.name}List = Array<${entity.name}>

export interface ${entity.name} extends EntityType {
<#list projectionFields as field>
<#if is_object(field.type)>
  string ${field.name}Id? = ${field?index + 1};
<#else>
  ${field.name}?: ${react_type(field.type)}
</#if>
</#list>
}

