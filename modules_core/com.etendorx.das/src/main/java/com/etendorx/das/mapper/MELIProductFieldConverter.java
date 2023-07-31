package com.etendorx.das.mapper;

import java.math.BigDecimal;

import org.openbravo.model.common.plm.Product;

public interface MELIProductFieldConverter {

  String getId(Product product);

  String getTitle(Product product);

  String getCategoryId(Product product);

  BigDecimal getPrice(Product product);

  String getCurrencyId(Product product);

  BigDecimal getAvailableQuantity(Product product);

  String getBuyingMode(Product product);

  String getCondition(Product product);

  String getListingTypeId(Product product);

  String getId(MELIProductDTO product);

  String getTitle(MELIProductDTO product);


}
