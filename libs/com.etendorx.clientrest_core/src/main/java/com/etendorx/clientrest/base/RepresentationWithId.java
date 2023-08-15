package com.etendorx.clientrest.base;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.hateoas.RepresentationModel;

import java.util.Date;

public abstract class RepresentationWithId<T extends RepresentationModel<T>> extends RepresentationModel<T> {
  public abstract String getId();

  @JsonProperty("updated")
  public Date getUpdated() {
    return null;
  }

  ;

  @JsonProperty("creationdate")
  public Date getCreationDate() {
    return null;
  }

  ;

}

