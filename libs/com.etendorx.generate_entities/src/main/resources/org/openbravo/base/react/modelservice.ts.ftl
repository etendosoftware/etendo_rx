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
import { BaseService } from "../base/baseservice"
import { ${entity.name}, ${entity.name}List, <#if searches??><#list searches as s>${s.method?cap_first}Params<#if !s?is_last>, </#if></#list></#if> } from "./${entity.name?lower_case}.types";

export class ${entity.name}Service extends BaseService<${entity.name}> {
  private static modelName = "${entity.name}";
  private static fetchName = "${entity.name?uncap_first}";

  getModelName(): string {
    return ${entity.name}Service.modelName;
  }
  getFetchName(): string {
    return ${entity.name}Service.fetchName;
  }

<#if searches??>

<#list searches as s>
  async ${s.method}(<#list s.params as p>${p.name}: ${react_type(p.type)}</#list>): Promise<${entity.name}List> {
    return this._fetchSearch<${s.method?cap_first}Params>("${s.method}", { <#list s.params as p>${p.name}: ${p.name}</#list> }, "${projectionName}")
  }
</#list>
</#if>

}

export default ${entity.name}Service;
