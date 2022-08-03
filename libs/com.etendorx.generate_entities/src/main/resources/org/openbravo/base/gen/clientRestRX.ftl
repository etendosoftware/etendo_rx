package ${packageClientRest?lower_case}.${entity.packageName};

import com.etendorx.clientrest.base.ClientRestBase;
import org.springframework.cloud.openfeign.FeignClient;
import com.etendorx.clientrest.base.FeignConfiguration;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name="${newClassName}", url = "${"$"}{das.url}/${newClassNameWithoutS?uncap_first}", configuration = FeignConfiguration.class)
public interface ${newClassName}ClientRest extends ClientRestBase<${packageEntityModel?lower_case}.${entity.packageName}.${newClassName}Model> {
<#list entity.properties as p>
    <#if p.targetEntity?? && !p.isOneToMany() && !p.isId() && !p.getTargetEntity().isView() && !p.computedColumn>
    @GetMapping("{${newClassName?lower_case}Id}/${p.javaName}")
    public ResponseEntity<${packageEntityModel?lower_case}.${p.getTableName(p.getObjectTypeName())}Model> find${p.javaName?cap_first}(@PathVariable(name = "${newClassName?lower_case}Id") String id);

    </#if>
</#list>

}
