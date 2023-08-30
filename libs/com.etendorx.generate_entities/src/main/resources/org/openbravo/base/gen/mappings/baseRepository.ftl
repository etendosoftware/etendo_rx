package com.etendorx.entities.mappings;

import com.etendorx.entities.entities.BaseDTORepositoryDefault;
import com.etendorx.entities.jparepo.${entity.name}Repository;
import ${entity.table.thePackage.javaPackage}.${entity.table.className};
import org.springframework.stereotype.Component;

@Component("${mappingPrefix}${entity.name}DASRepository")
public class ${mappingPrefix}${entity.name}DTORepositoryDefault extends BaseDTORepositoryDefault<${entity.table.className}, ${mappingPrefix}${entity.name}DTORead, ${mappingPrefix}${entity.name}DTOWrite> {

  public ${mappingPrefix}${entity.name}DTORepositoryDefault(${entity.name}Repository repository,
    ${mappingPrefix}${entity.name}DTOConverter converter) {
    super(repository, converter);
  }

}
