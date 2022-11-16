<#-- @formatter:off -->
<#assign x = ["ad_ref_table","ad_clientinfo", "ad_orginfo", "obkmo_widget_reference"]>
<#assign noAuditTables = ["ad_ref_table","ad_clientinfo", "ad_orginfo", "obkmo_widget_reference", "ad_org", "ad_client"]>
<#assign auditFields = ["ad_org_id", "ad_client_id", "isactive", "created", "createdby", "updated", "updatedby"]>

package ${entity.packageName};

import com.etendorx.entities.entities.BaseRXObject;
import com.fasterxml.jackson.annotation.JsonProperty;

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
public class ${entity.simpleClassName} <#if noAuditTables?seq_contains(entity.tableName?lower_case)>implements Serializable<#else>extends BaseRXObject</#if> {
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
    <#if entity.isView() >
    <#list entity.properties as o>
    <#if o.columnName?? && o.columnName = p.columnName && o.isId() >
    <#assign repeated=true/>
    </#if>
    </#list>
    </#if>
    <#if x?seq_contains(entity.tableName?lower_case)>
    @javax.persistence.JoinColumn(name = "${p.columnName?lower_case}_", referencedColumnName = "${p.getTargetEntity().getTableName()}_id")
    <#else>
    @javax.persistence.JoinColumn(name = "${p.columnName?lower_case}", referencedColumnName = "${p.getTargetEntity().getTableName()}_id"<#if repeated>, updatable = false, insertable = false</#if>)
    </#if>
    @javax.persistence.ManyToOne(fetch=javax.persistence.FetchType.LAZY)
    @JsonProperty("${p.javaName}")
    ${p.targetEntity.className} ${p.javaName};

    <#else>
    <#if p.targetEntity?? && !p.isId() && !p.getTargetEntity().isView()>
    //@javax.persistence.OneToMany(cascade = javax.persistence.CascadeType.ALL, mappedBy = "${p.referencedProperty.name}")
    //java.util.List<${packageName}.${p.getTableName(p.getObjectTypeName())}> ${p.name};

    </#if>
    </#if>
    </#if>
    </#if>
    </#if>
    <#else>
    <#if computedColumns && p.computedColumn && !p.targetEntity??><#assign query>${p.sqlLogic}</#assign>
    @Formula("(${query?replace("^\\s+|\\s+$|\\n|\\r", " ", "rm")?replace("\"", "\\\"")})")
    @JsonProperty("${p.javaName}")
    ${p.getObjectTypeName()} ${p.javaName};

    </#if>
    </#if>
    </#list>

}
