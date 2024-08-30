<#function is_object string>
 <#if string == "java.lang.String" || string == "String" || string == "java.math.BigDecimal" || string == "java.lang.Long" || string == "java.util.Date" || string == "java.lang.Boolean" || string == "java.sql.Timestamp"><#return false><#else><#return true></#if>
</#function>
<#function react_type string>
 <#switch string>
  <#case "java.lang.String"><#return "string"><#break>
  <#case "String"><#return "string"><#break>
  <#case "java.math.BigDecimal"><#return "number"><#break>
  <#case "java.lang.Long"><#return "number"><#break>
  <#case "java.util.Date"><#return "Date"><#break>
  <#case "java.sql.Timestamp"><#return "Date"><#break>
  <#case "java.lang.Boolean"><#return "boolean"><#break>
  <#default><#return string>
 </#switch>
</#function>
import {BaseService} from '../base/baseservice';
import {${entity.name}, ${entity.name}List, <#if searches??><#list searches as s>${s.method?cap_first}Params<#if !s?is_last>, </#if></#list></#if>} from './${entity.name?lower_case}.types';

class BackService extends BaseService<${entity.name}> {
  private static projection = '${projectionName?lower_case}';
  private static modelName = '${externalName}';
  private static fetchName = '${entity.name?uncap_first}';

  getModelName(): string {
    return BackService.projection + "/" + BackService.modelName;
  }
  getFetchName(): string {
    return BackService.fetchName;
  }

<#if searches??>

<#list searches as s>
  async ${s.method}(
  <#list s.params as p>  ${p.name}: ${react_type(p.type)},
  </#list>  page?: number,
    size?: number,
  ): Promise<${entity.name}List> {
    return this._fetchSearch<${s.method?cap_first}Params>('${s.method}', {
      <#list s.params as p>${p.name}, </#list>
      page,
      size,
    });
  }
</#list>
</#if>

}

class FrontService extends BaseService<${entity.name}> {
  getModelName(): string {
    throw new Error('Method not implemented.');
  }
  getFetchName(): string {
    throw new Error('Method not implemented.');
  }
  mapManyToOne(entity: ${entity.name}): void {
    throw new Error('Method not implemented.');
  }
}

export default class ${entity.name}Service {
  static BACK = new BackService();
  static FRONT = new FrontService();
}
