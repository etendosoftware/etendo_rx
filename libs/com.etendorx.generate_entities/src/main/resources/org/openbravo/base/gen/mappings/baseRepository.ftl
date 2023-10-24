package com.etendorx.entities.mappings;

import com.etendorx.entities.entities.AuditServiceInterceptor;
import com.etendorx.entities.entities.BaseDTORepositoryDefault;
import com.etendorx.entities.jparepo.${entity.name}Repository;
import com.etendorx.eventhandler.transaction.RestCallTransactionHandler;
import ${entity.table.thePackage.javaPackage}.${entity.table.className};
import com.etendorx.entities.mapper.lib.JsonPathEntityRetriever;
import org.springframework.stereotype.Component;

@Component("${mappingPrefix}${entity.name}DASRepository")
public class ${mappingPrefix}${entity.name}DTORepositoryDefault extends BaseDTORepositoryDefault<${entity.table.className}, ${mappingPrefix}${entity.name}DTORead, ${mappingPrefix}${entity.name}DTOWrite> {

  public ${mappingPrefix}${entity.name}DTORepositoryDefault(
      RestCallTransactionHandler restCallTransactionHandler,
      ${entity.name}Repository repository,
      ${mappingPrefix}${entity.name}DTOConverter converter,
      ${mappingPrefix}${entity.name}JsonPathRetriever retriever,
      AuditServiceInterceptor auditService
    ) {
    super(restCallTransactionHandler, repository, converter, retriever, auditService);
  }

}
