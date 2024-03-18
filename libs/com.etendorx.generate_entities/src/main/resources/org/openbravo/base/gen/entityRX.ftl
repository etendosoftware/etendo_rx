<#-- @formatter:off -->
<#assign x = ["ad_ref_table","ad_clientinfo", "ad_orginfo", "obkmo_widget_reference"]>
<#assign noAuditTables = ["ad_ref_table","ad_clientinfo", "ad_orginfo", "obkmo_widget_reference", "ad_org", "ad_client", "c_salary_category_cost"]>
<#assign auditFields = ["ad_org_id", "ad_client_id", "isactive", "created", "createdby", "updated", "updatedby"]>

package ${entity.packageName};

import com.etendorx.entities.entities.BaseSerializableObject;
import com.etendorx.entities.entities.BaseRXObject;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Convert;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import org.hibernate.type.YesNoConverter;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
*
* ${newClassName} EtendoRX model class
*
*/

@Getter
@Setter
@jakarta.persistence.Entity(name = "${entity.name}")
@jakarta.persistence.Table(name = "${entity.tableName?lower_case}")
@jakarta.persistence.Cacheable
@EntityScan
public class ${entity.simpleClassName} <#if noAuditTables?seq_contains(entity.tableName?lower_case)>implements BaseSerializableObject<#else>extends BaseRXObject</#if> {

  public static final String TABLE_ID = "${entity.tableId}";

<#list entity.properties as p>
    <#if !p.computedColumn>
        <#if p.isId()>
    @jakarta.persistence.Id
            <#if entity.help?? == false || entity.help != "{@literal identity=NONE}">
    @jakarta.persistence.GeneratedValue(generator = "custom-generator")
    @org.hibernate.annotations.GenericGenerator(
    name = "custom-generator",
        strategy = "com.etendorx.entities.utilities.UUIDGenerator"
    )
            </#if>
    @jakarta.persistence.Column(name = "${p.columnName?lower_case}"<#if entity.isView()>, insertable = false, updatable = false</#if>)
            <#if p.isMandatory()>
    @NotNull
           </#if>
    @JsonProperty("${p.javaName}")
    java.lang.String ${p.javaName};

        </#if>
        <#if p.columnName?? && (noAuditTables?seq_contains(entity.tableName?lower_case) || !auditFields?seq_contains(p.columnName?lower_case))>
            <#if p.isPrimitive() && !p.isId()>
                <#if !p.getPrimitiveType().isArray()>
    @jakarta.persistence.Column(name = "${p.columnName?lower_case}")
                  <#if p.isMandatory()>
    @NotNull
                  </#if>
                    <#if p.isBoolean()>
    @Convert(converter = YesNoConverter.class)
                    </#if>
                    <#if p.javaName == "version">
    @JsonProperty("${p.javaName}_Etendo")
    ${p.getObjectTypeName()} ${p.javaName}_Etendo;
                    <#else>
    @JsonProperty("${p.javaName}")
                      <#if p.getFormattedDefaultValue()??>
    ${p.getObjectTypeName()} ${p.javaName} = ${p.getFormattedDefaultValue()};
                        <#else>
    ${p.getObjectTypeName()} ${p.javaName};
                        </#if>
                    </#if>
                <#else>
    @jakarta.persistence.Column(name = "${p.columnName?lower_case}")
                  <#if p.isMandatory()>
    @NotNull
                  </#if>
    @JsonProperty("${p.javaName}")
    ${p.shorterTypeName} ${p.javaName};

                </#if>
            <#else>
                <#if p.targetEntity?? && !p.isOneToMany() && !p.isId()>
                    <#if p.targetEntity?? >
                        <#assign repeated=false/>
                        <#list entity.properties as o>
                            <#if o.columnName?? && o.javaName != p.javaName && o.columnName?lower_case == p.columnName?lower_case>
                                <#assign repeated=true/>
                            </#if>
                        </#list>
    @jakarta.persistence.JoinColumn(name = "${p.columnName?lower_case}", referencedColumnName = "${p.referencedProperty.columnName?lower_case}"<#if repeated>, updatable = false, insertable = false</#if>)
    @jakarta.persistence.ManyToOne(fetch=jakarta.persistence.FetchType.LAZY)
    @JsonProperty("${p.javaName}")
                      <#if p.isMandatory()>
      @NotNull
                      </#if>
    ${p.targetEntity.className} ${p.javaName};

                    <#else>
                    </#if>
                </#if>
            </#if>
        <#else>
            <#if p.oneToMany && p.targetEntity?? && !p.isId() && !p.targetEntity.className?ends_with("_ComputedColumns")>
    @jakarta.persistence.OneToMany(mappedBy = "${p.referencedProperty.name}", cascade = jakarta.persistence.CascadeType.ALL)
    @JsonIgnoreProperties("${p.referencedProperty.name}")
    java.util.List<${p.targetEntity.className}> ${p.name};

            </#if>
        </#if>
    <#else>
        <#if computedColumns && p.computedColumn && !p.targetEntity??><#assign query>${p.sqlLogic}</#assign>
    @Formula("(${query?replace("^\\s+|\\s+$|\\n|\\r", " ", "rm")?replace("\"", "\\\"")})")
    @JsonProperty("${p.javaName}")
    @JsonIgnore
    ${p.getObjectTypeName()} ${p.javaName};

        </#if>
    </#if>
</#list>

    @Override
    public String get_identifier() {
        return id;
    }
}
