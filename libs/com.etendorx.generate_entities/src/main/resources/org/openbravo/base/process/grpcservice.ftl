<#function is_object string>
    <#if string == "java.lang.String" || string == "String" || string == "java.math.BigDecimal" || string == "java.lang.Long" || string == "java.util.Date" || string == "java.lang.Boolean" || string == "java.sql.Timestamp"><#return false><#else><#return true></#if>
</#function>
<#function cast_par p>
    -- ${p.type} --
    <#if p.type == "java.util.Date">new Date(request.get${p.name?cap_first}().getSeconds())<#else>request.get${p.name?cap_first}()</#if>
</#function>
/*
* Copyright 2022  Futit Services SL
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.etendorx.das.grpcrepo;

import com.etendorx.entities.jparepo.${name}Repository;
import com.etendorx.${projectionName}.grpc.*;
import com.google.protobuf.util.Timestamps;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Date;

@GrpcService
@Slf4j
public class ${name}GrpcService extends ${name}GrpcRepositoryGrpc.${name}GrpcRepositoryImplBase {
    @Autowired
    ${name}Repository repository;

    private ${name} DTO(${name}.Builder modelBuilder, ${className} p) {
        modelBuilder.clear();
    <#list fields as field>
        <#if field.projectedEntity??>
        <#if field.projectedEntity == "target">
        if(p.get${field.projectedField?cap_first}() != null)
        <#if field.type == "java.math.BigDecimal">
            modelBuilder.set${field.name?cap_first}(p.get${field.projectedField?cap_first}().doubleValue());
        <#elseif field.type == "java.util.Date">
            modelBuilder.set${field.name?cap_first}(Timestamps.fromMillis(p.get${field.name?cap_first}().getTime()));
        <#else>
            modelBuilder.set${field.name?cap_first}(p.get${field.projectedField?cap_first}());
        </#if>
        <#else>
        if(p.get${field.projectedEntity?cap_first}() != null && p.get${field.projectedEntity?cap_first}().get${field.projectedField?cap_first}() != null)
        <#if field.type == "java.math.BigDecimal">
            modelBuilder.set${field.name?cap_first}(p.get${field.projectedEntity?cap_first}().get${field.projectedField?cap_first}().doubleValue());
        <#elseif field.type == "java.util.Date">
            modelBuilder.set${field.name?cap_first}(Timestamps.fromMillis(p.get${field.projectedEntity?cap_first}().get${field.projectedField?cap_first}().getTime()));
        <#else>
            modelBuilder.set${field.name?cap_first}(p.get${field.projectedEntity?cap_first}().get${field.projectedField?cap_first}());
        </#if>
        </#if>
        <#elseif is_object(field.type)>
        if(p.get${field.name?cap_first}() != null)
            modelBuilder.set${field.name?cap_first}Id(p.get${field.name?cap_first}().getId());
        <#else>
        // field.value: <#if field.value??>${field.value}<#else>null</#if>
        <#if field.notNullValue??>
        if(${field.notNullValue?replace('#TARGET#', 'p')})
        <#if field.type == "java.math.BigDecimal">
            modelBuilder.set${field.name?cap_first}(p.get${field.value?cap_first}().doubleValue());
        <#elseif field.type == "java.util.Date">
            modelBuilder.set${field.name?cap_first}(Timestamps.fromMillis(p.get${field.value?cap_first}().getTime()));
        <#else>
            modelBuilder.set${field.name?cap_first}(p${field.value});
        </#if>
        <#else>
        if(p.get<#if field.value??>${field.value?cap_first}<#else>${field.name?cap_first}</#if>() != null)
        <#if field.type == "java.math.BigDecimal">
            modelBuilder.set${field.name?cap_first}(p.get<#if field.value??>${field.value?cap_first}<#else>${field.name?cap_first}</#if>().doubleValue());
        <#elseif field.type == "java.util.Date">
            modelBuilder.set${field.name?cap_first}(Timestamps.fromMillis(p.get<#if field.value??>${field.value?cap_first?cap_first}<#else>${field.name?cap_first}</#if>().getTime()));
        <#else>
            modelBuilder.set${field.name?cap_first}(p.get<#if field.value??>${field.value?cap_first}<#else>${field.name?cap_first}</#if>());
        </#if>
        </#if>
        </#if>
    </#list>
        return modelBuilder.build();
    }
<#if transactional>

    private ${packageName}.${name} DTO(
    ${packageName}.${name} entity,
    ${name} entityModel) {
    <#list fields as field>
        <#if is_object(field.type)>
            /*
            if(entityModel.getUserContactId() != "") {
            ${field.className} ${field.name} = null;
            var ${field.name}Optional = ${field.type}Repository.findById( entityModel.get${field.name}Id() );
            if(${field.name}Optional.isPresent()) {
            ${field.name} = ${field.name}Optional.get();
            }
            entity.set${field.name?cap_first}(${field.name});
            }
            */
        <#else>
            <#if field.type == "java.math.BigDecimal">
                entity.set${field.name?cap_first}(new BigDecimal(entityModel.get${field.name?cap_first}()));
            <#elseif field.type == "java.util.Date">
                entity.set${field.name?cap_first}(new Date(entityModel.get${field.name?cap_first}().getSeconds()));
            <#else>
                entity.set${field.name?cap_first}(entityModel.get${field.name?cap_first}());
            </#if>
        </#if>
    </#list>
    return entity;
    }
</#if>

<#list searches as search>
    @Override
    public void ${search.method}(${name}_${search.method}Search request,  StreamObserver
    <${name}List> responseObserver) {
        log.debug("Request " + request);
        ${name}List.Builder listBuilder = ${name}List.newBuilder();
        ${name}.Builder modelBuilder = ${name}.newBuilder();
        repository.${search.method}(
            <#list search.params as p><#if p.type?? && p.type == "java.util.Date">new Date(request.get${p.name?cap_first}().getSeconds())<#else>request.get${p.name?cap_first}()</#if>, </#list>
            null
        ).map( p -> DTO(modelBuilder, p))
        .forEach(listBuilder::add${name?lower_case?cap_first});
        listBuilder.setSize(listBuilder.get${name?lower_case?cap_first}Count());
        responseObserver.onNext(listBuilder.build());
        responseObserver.onCompleted();
    }

    </#list>

    <#if transactional>
        @Override
        public void save(${name}_saveParam request, StreamObserver<${name}> responseObserver) {
        ${packageName}.${name} entity = null;
        if(request.getEntity().getId().compareTo("") == 0) {
        var entityOpt = repository.findById(request.getEntity().getId());
        if(entityOpt.isPresent()) {
        entity = entityOpt.get();
        }
        }
        if(entity == null) {
        entity = new ${packageName}.${name}();
        }
        var savedEntity = repository.save( DTO(entity, request.getEntity()) );
        responseObserver.onNext(DTO(${name}.newBuilder(), savedEntity));
        responseObserver.onCompleted();
    }

    </#if>

}
