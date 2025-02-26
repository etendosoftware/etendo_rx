package com.etendorx.entities.mappings;

import com.etendorx.entities.entities.AuditServiceInterceptor;
import com.etendorx.entities.entities.BaseDTORepositoryDefault;
import com.etendorx.entities.jparepo.${entity.table.name}Repository;
import com.etendorx.entities.mapper.lib.DefaultValuesHandler;
import com.etendorx.entities.mapper.lib.ExternalIdService;
import com.etendorx.entities.mapper.lib.PostSyncService;
import com.etendorx.eventhandler.transaction.RestCallTransactionHandler;
import jakarta.validation.Validator;
import ${entity.table.thePackage.javaPackage}.${entity.table.className};
import com.etendorx.entities.mapper.lib.JsonPathEntityRetriever;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("${mappingPrefix}${entity.externalName}DASRepository")
public class ${mappingPrefix}${entity.externalName}DTORepositoryDefault extends BaseDTORepositoryDefault<${entity.table.className}, ${mappingPrefix}${entity.externalName}DTORead, ${mappingPrefix}${entity.externalName}DTOWrite> {

  public ${mappingPrefix}${entity.externalName}DTORepositoryDefault(
      RestCallTransactionHandler restCallTransactionHandler,
      ${entity.table.name}Repository repository,
      ${mappingPrefix}${entity.externalName}DTOConverter converter,
      ${mappingPrefix}${entity.externalName}JsonPathRetriever retriever,
      AuditServiceInterceptor auditService,
      Validator validator,
      ExternalIdService externalIdService,
      Optional<DefaultValuesHandler> defaultValuesHandler,
      PostSyncService postSyncService
    ) {
    super(restCallTransactionHandler, repository, converter, retriever, auditService, validator, externalIdService, defaultValuesHandler, postSyncService);
  }

  <#list modelProviderRX.getETRXRepositories(entity) as repo>
    <#list repo.searches as search>
  Page<${mappingPrefix}${entity.externalName}DTORead> ${search.method}(<#list search.params as param>${param.type} ${param.name}, </#list>Pageable page) {
      var repository = ((${entity.table.name}Repository) getRepository()).${search.method}(<#list search.params as param>${param.name}, </#list>page);
      return getConverter().convert(repository);
  }
    </#list>
  </#list>
}
