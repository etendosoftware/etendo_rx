package com.etendorx.gen.generation;

import com.etendoerp.etendorx.model.repository.ETRXEntitySearch;
import com.etendoerp.etendorx.model.repository.ETRXRepository;
import com.etendoerp.etendorx.model.repository.ETRXSearchParam;
import com.etendorx.gen.beans.Repository;
import com.etendorx.gen.beans.RepositorySearch;
import com.etendorx.gen.beans.RepositorySearchParam;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RepositoriesConverter {

  /**
   * Converts the ETRX repositories to repositories
   *
   * @param etrxRepositories
   * @return
   */
  public List<Repository> convert(List<ETRXRepository> etrxRepositories) {
    List<Repository> repositories = new ArrayList<>();
    for (ETRXRepository etrxRepository : etrxRepositories) {
      Repository repository = new Repository(etrxRepository.getEntityName(), true);
      for (ETRXEntitySearch search : etrxRepository.getSearches()) {
        Map<String, RepositorySearchParam> params = new LinkedHashMap<>();
        for (ETRXSearchParam etrxSearchParam : search.getParams()) {
          RepositorySearchParam param = new RepositorySearchParam(etrxSearchParam.getName(),
              etrxSearchParam.getType());
          params.put(param.getName(), param);
        }
        RepositorySearch repositorySearch = new RepositorySearch(search.getMethod(),
            search.getQuery(), params);
        repository.getSearches().put(repositorySearch.getMethod(), repositorySearch);
      }
      repositories.add(repository);
    }
    return repositories;
  }
}
