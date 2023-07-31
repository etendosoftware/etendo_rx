package com.etendorx.das.mapper;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonKey;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MELIProductDTO {
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
}
