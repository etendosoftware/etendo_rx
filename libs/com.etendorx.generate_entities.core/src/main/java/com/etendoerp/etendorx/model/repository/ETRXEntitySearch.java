package com.etendoerp.etendorx.model.repository;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import org.openbravo.base.model.ModelObject;

import java.util.Set;

@JsonIncludeProperties({"query", "method", "params"})
public class ETRXEntitySearch extends ModelObject {

  private ETRXRepository repository;
  private String query;
  private String method;
  private Set<ETRXSearchParam> params;

  public ETRXRepository getRepository() {
    return repository;
  }

  public void setRepository(ETRXRepository repository) {
    this.repository = repository;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public Set<ETRXSearchParam> getParams() {
    return params;
  }

  public void setParams(Set<ETRXSearchParam> params) {
    this.params = params;
  }
}
