<#function is_object string>
 <#if string == "java.lang.String" || string == "String" || string == "java.math.BigDecimal" || string == "java.lang.Long" || string == "java.util.Date" || string == "java.lang.Boolean" || string == "java.sql.Timestamp"><#return false><#else><#return true></#if>
</#function>
import { useState } from 'react';
import { ${entity.name}, ${entity.name}List } from '../data_gen/${entity.name?lower_case}.types';
import ${entity.name}Service from '../data_gen/${entity.name?lower_case}service';

export const use${entity.name} = () => {
  const [${entity.name}sFiltered, set${entity.name}sFiltered] = useState<${entity.name}List | null>(
    null,
  );
  const getFiltered${entity.name}s = async (
    name: string = '%%',
    page?: number,
    size?: number,
  ) => {
    const ${entity.name}s: ${entity.name}List = await ${entity.name}Service.BACK.getFiltered${entity.name}s(
      name,
      page,
      size,
    );
    set${entity.name}sFiltered(${entity.name}s);
    return ${entity.name}s;
  };

  const create${entity.name} = async (body: ${entity.name}) => {
  <#list projectionFields as field>
  <#if is_object(field.type)>
    body.${field.name} = body.${field.name} ?? null;
  </#if>
  </#list>
  <#list projectionFields as field>
    <#if field.name == "active">
    body.active = true;
    </#if>
  </#list>
    await ${entity.name}Service.BACK.save(body);
  };

  const update${entity.name} = async (body: ${entity.name}) => {
   <#list projectionFields as field>
     <#if is_object(field.type)>
    body.${field.name} = body.${field.name} ?? null;
     </#if>
     </#list>
     <#list projectionFields as field>
       <#if field.name == "active">
    body.active = body.active ?? true;
       </#if>
     </#list>
    const res = await ${entity.name}Service.BACK.save(body);
    return res;
  };

  return {
    ${entity.name}sFiltered,
    getFiltered${entity.name}s,
    create${entity.name},
    update${entity.name},
  };
};

export default use${entity.name};
