package com.etendorx.entities.mappings;

import com.etendorx.entities.entities.AuditServiceInterceptor;
import com.etendorx.entities.entities.BaseDTORepositoryDefault;
import com.etendorx.entities.jparepo.${entity.table.name}Repository;
import com.etendorx.eventhandler.transaction.RestCallTransactionHandler;
import ${entity.table.thePackage.javaPackage}.${entity.table.className};
import com.etendorx.entities.mapper.lib.JsonPathEntityRetriever;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component("${mappingPrefix}${entity.externalName}DASRepository")
public class ${mappingPrefix}${entity.externalName}DTORepositoryDefault extends BaseDTORepositoryDefault<${entity.table.className}, ${mappingPrefix}${entity.externalName}DTORead, ${mappingPrefix}${entity.externalName}DTOWrite> {

  public ${mappingPrefix}${entity.externalName}DTORepositoryDefault(
      RestCallTransactionHandler restCallTransactionHandler,
      ${entity.table.name}Repository repository,
      ${mappingPrefix}${entity.externalName}DTOConverter converter,
      ${mappingPrefix}${entity.externalName}JsonPathRetriever retriever,
      AuditServiceInterceptor auditService
    ) {
    super(restCallTransactionHandler, repository, converter, retriever, auditService);
  }

  <#list modelProviderRX.getETRXRepositories(entity) as repo>
    <#list repo.searches as search>
  Page<${mappingPrefix}${entity.externalName}DTORead> ${search.method}(<#list search.params as param>String ${param.name}, </#list>Pageable page) {
      var repository = ((${entity.table.name}Repository) getRepository()).${search.method}(<#list search.params as param>${param.name}, </#list>page);
      return getConverter().convert(repository);
  }
    </#list>
  </#list>
}
