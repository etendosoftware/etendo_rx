package com.etendorx.das.projections;

import org.openbravo.model.common.businesspartner.Category;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

@Projection(name = "custom", types = org.openbravo.model.common.businesspartner.BusinessPartner.class)
public class BusinessPartnerCustomProjection {
  java.lang.String name;

  @JsonIgnore
  Category category;

  private String getCategoryName() {
    return category.getName();
  }

}
