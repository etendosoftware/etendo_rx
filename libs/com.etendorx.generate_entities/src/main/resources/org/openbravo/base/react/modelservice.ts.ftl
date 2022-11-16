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
import {BaseService} from '../base/baseservice'
import {
  ${entity.name},
  ${entity.name}List,
  <#if searches??><#list searches as s>${s.method?cap_first}Params<#if !s?is_last>, </#if></#list></#if>
} from "./${entity.name?lower_case}.types";

class BackService extends BaseService<${entity.name}> {
  private static modelName = '${entity.name}';
  private static fetchName = '${entity.name?uncap_first}';

  getModelName(): string {
    return BackService.modelName;
  }
  getFetchName(): string {
    return BackService.fetchName;
  }

  mapManyToOne(entity: ${entity.name}): void {
  <#list projectionFields as field>
  <#if is_object(field.type)>
    if (entity.bpartnerId) {
      entity.${field.name} = `${field.name}/${'$'}{entity.${field.name}Id}`;
    }
  </#if>
  </#list>
  }
<#if searches??>

<#list searches as s>
  async ${s.method}(<#list s.params as p>${p.name}: ${react_type(p.type)}</#list>): Promise<${entity.name}List> {
    return this._fetchSearch<${s.method?cap_first}Params>(
      '${s.method}',
      {<#list s.params as p>${p.name}: ${p.name}</#list>},
      '${projectionName}',
    )
  }
</#list>
</#if>

}

class FrontService {
  async save(data: ${entity.name}): Promise<${entity.name}> {
    const req = await fetch(
      `/api/${entity.name}/${'$'}{data.id}`,
      {
        method: 'POST',
        body: JSON.stringify(data),
      },
    );
    return await req.json();
  }
}

export default class ${entity.name}Service {
  static BACK = new BackService();
  static FRONT = new FrontService();
}
