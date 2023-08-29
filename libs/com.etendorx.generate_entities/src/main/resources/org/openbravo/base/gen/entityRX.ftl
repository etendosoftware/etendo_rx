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

import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import org.springframework.boot.autoconfigure.domain.EntityScan;

import java.io.Serializable;

/**
*
* ${newClassName} EtendoRX model class
*
*/

@Getter
@Setter
@javax.persistence.Entity(name = "${entity.name}")
@javax.persistence.Table(name = "${entity.tableName?lower_case}")
@javax.persistence.Cacheable
@EntityScan
public class ${entity.simpleClassName} <#if noAuditTables?seq_contains(entity.tableName?lower_case)>implements BaseSerializableObject<#else>extends BaseRXObject</#if> {
<#list entity.properties as p>
    <#if !p.computedColumn>
        <#if p.isId()>
    @javax.persistence.Id
            <#if entity.help?? == false || entity.help != "{@literal identity=NONE}">
    @javax.persistence.GeneratedValue(generator = "custom-generator")
    @org.hibernate.annotations.GenericGenerator(
    name = "custom-generator",
        strategy = "com.etendorx.entities.utilities.UUIDGenerator"
    )
            </#if>
    @javax.persistence.Column(name = "${p.columnName?lower_case}"<#if entity.isView()>, insertable = false, updatable = false</#if>)
    @JsonProperty("${p.javaName}")
    java.lang.String ${p.javaName};

        </#if>
        <#if p.columnName?? && (noAuditTables?seq_contains(entity.tableName?lower_case) || !auditFields?seq_contains(p.columnName?lower_case))>
            <#if p.isPrimitive() && !p.isId()>
                <#if !p.getPrimitiveType().isArray()>
    @javax.persistence.Column(name = "${p.columnName?lower_case}")
                    <#if p.isBoolean()>
    @javax.persistence.Convert(converter= com.etendorx.entities.utilities.BooleanToStringConverter.class)
                    </#if>
                    <#if p.javaName == "version">
    @JsonProperty("${p.javaName}_Etendo")
    ${p.getObjectTypeName()} ${p.javaName}_Etendo;
                    <#else>
    @JsonProperty("${p.javaName}")
    ${p.getObjectTypeName()} ${p.javaName};
                    </#if>
                <#else>
    @javax.persistence.Column(name = "${p.columnName?lower_case}")
    @JsonProperty("${p.javaName}")
    ${p.shorterTypeName} ${p.javaName};

                </#if>
            <#else>
                <#if p.targetEntity?? && !p.isOneToMany() && !p.isId() && !p.getTargetEntity().isView()>
                    <#if p.targetEntity?? >
                        <#assign repeated=false/>
                        <#list entity.properties as o>
                            <#if o.columnName?? && o.javaName != p.javaName && o.columnName?lower_case == p.columnName?lower_case>
                                <#assign repeated=true/>
                            </#if>
                        </#list>
    @javax.persistence.JoinColumn(name = "${p.columnName?lower_case}", referencedColumnName = "${p.referencedProperty.columnName?lower_case}"<#if repeated>, updatable = false, insertable = false</#if>)
    @javax.persistence.ManyToOne(fetch=javax.persistence.FetchType.LAZY)
    @JsonProperty("${p.javaName}")
    ${p.targetEntity.className} ${p.javaName};

                    <#else>
                    </#if>
                </#if>
            </#if>
        <#else>
            <#if p.oneToMany && p.targetEntity?? && !p.isId() && !p.getTargetEntity().isView() && !p.targetEntity.className?ends_with("_ComputedColumns")>
    @javax.persistence.OneToMany(mappedBy = "${p.referencedProperty.name}")
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
