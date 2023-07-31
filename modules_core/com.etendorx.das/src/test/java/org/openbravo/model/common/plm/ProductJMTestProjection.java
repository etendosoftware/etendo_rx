package org.openbravo.model.common.plm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.core.config.Projection;

import com.fasterxml.jackson.annotation.JsonProperty;

@Projection(name = "mapping-test", types = Product.class)
public interface ProductJMTestProjection {
  @Value("#{target.getName()}")
  Object getTitle();

  @Value("#{@mproductTestMapping.getCategoryId(target)}")
  @JsonProperty("category_id")
  Object getCategoryId();

  @Value("#{@mproductTestMapping.getPrice(target)}")
  @JsonProperty("price")
  Object getPrice();

  @Value("#{@mproductTestMapping.getCurrencyId(target)}")
  @JsonProperty("currency_id")
  Object getCurrencyId();

  @Value("#{@mproductTestMapping.getAvailableQuantity(target)}")
  @JsonProperty("available_quantity")
  Object getAvailableQuantity();

  @Value("#{@mproductTestMapping.getBuyingMode(target)}")
  @JsonProperty("buying_mode")
  Object getBuyingMode();

  @Value("#{@mproductTestMapping.getCondition(target)}")
  @JsonProperty("condition")
  Object getCondition();

  @Value("#{@mproductTestMapping.getListingTypeId(target)}")
  @JsonProperty("listing_type_id")
  Object getListingTypeId();

}
