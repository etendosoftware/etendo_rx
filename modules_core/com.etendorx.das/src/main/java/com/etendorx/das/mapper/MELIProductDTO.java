package com.etendorx.das.mapper;

import java.math.BigDecimal;

import com.etendorx.entities.entities.BaseDTOModel;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MELIProductDTO implements BaseDTOModel {
  @JsonProperty("id")
  String id;
  @JsonProperty("title")
  String title;
  @JsonProperty("category_id")
  String categoryId;
  @JsonProperty("price")
  BigDecimal price;
  @JsonProperty("currency_id")
  String currencyId;
  @JsonProperty("available_quantity")
  BigDecimal availableQuantity;
  @JsonProperty("buying_mode")
  String buyingMode;
  @JsonProperty("condition")
  String condition;
  @JsonProperty("listing_type_id")
  String listingTypeId;
  @JsonProperty("sale_terms")
  Object saleTerms;
  @JsonProperty("pictures")
  Object pictures;
  @JsonProperty("attributes")
  Object attributes;
  @JsonProperty("stocked")
  Boolean stocked;
}
