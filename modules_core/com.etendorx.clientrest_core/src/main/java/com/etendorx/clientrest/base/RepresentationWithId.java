package com.etendorx.clientrest.base;

import org.springframework.hateoas.RepresentationModel;

import java.util.Date;

public abstract class RepresentationWithId<T extends RepresentationModel<T>> extends RepresentationModel<T> {
  public abstract String getId();

  public Date getUpdated() {
    return null;
  };

  public Date getCreationDate() {
    return null;
  };

}

