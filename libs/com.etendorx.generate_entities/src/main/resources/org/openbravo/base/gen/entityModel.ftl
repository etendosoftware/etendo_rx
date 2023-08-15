<#function getter p>
  <#if p.boolean>
    <#return "is" + p.getterSetterName?cap_first>
  <#else>
    <#return "get" + p.getterSetterName?cap_first>
  </#if>
</#function>
package ${packageEntityModel?lower_case}.${entity.packageName};

import ${packageClientRest?lower_case}.${entity.packageName}.${newClassName}ClientRest;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.etendorx.clientrest.base.RepresentationWithId;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.etendorx.clientrest.base.EntitySerialize;
import com.etendorx.clientrest.base.SpringContext;
import org.springframework.http.ResponseEntity;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ${newClassName}Model extends RepresentationWithId<${newClassName}Model> {
  @JsonIgnore
  private ${newClassName}ClientRest restClient = SpringContext.getBean(${newClassName}ClientRest.class);

  <#list entity.properties as p>
  <#if !p.computedColumn>
  <#if p.isId()>
  private java.lang.String ${p.javaName};

  </#if>
  <#if p.isPrimitive() && !p.isId()>
  <#if !p.getPrimitiveType().isArray()>
  <#if p.javaName == "version">
  private ${p.getObjectTypeName()} ${p.javaName}_Etendo;

  <#else>
  private ${p.getObjectTypeName()} ${p.javaName};

  </#if>
  <#else>
  private ${p.shorterTypeName} ${p.javaName}_Etendo;

  </#if>
  <#else>
    <#if p.targetEntity?? && !p.isOneToMany() && !p.isId() && !p.getTargetEntity().isView()>
      <#if p.targetEntity?? >
        @JsonSerialize(using = EntitySerialize.class)
        ${packageEntityModel}.${p.getTableName(p.getObjectTypeName())}Model ${p.javaName};

      <#else>
      </#if>
    </#if>
  </#if>
  </#if>
  </#list>

  @Override
  public String getId() {
    return id;
  }

  <#list entity.properties as p>
    <#if p.targetEntity?? && !p.isOneToMany() && !p.isId() && !p.getTargetEntity().isView() && !p.computedColumn>

      @JsonIgnore
      public ResponseEntity<${packageEntityModel}.${p.getTableName(p.getObjectTypeName())}Model> get${p.javaName?cap_first}() {
      if (${p.javaName} == null) {
      try {
      return restClient.find${p.javaName?cap_first}(this.getId());
      } catch (Exception e) {
      return ResponseEntity.ok(${p.javaName});
      }
      }
      return ResponseEntity.ok(${p.javaName});
      }

      @JsonSetter
      public void set${p.javaName?cap_first}(${packageEntityModel}.${p.getTableName(p.getObjectTypeName())}Model ${p.javaName}) {
      this.${p.javaName} = ${p.javaName};
      }
    </#if>
  </#list>

}
