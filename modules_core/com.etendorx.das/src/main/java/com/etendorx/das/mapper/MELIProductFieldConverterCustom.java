package com.etendorx.das.mapper;

import java.math.BigDecimal;

import org.jetbrains.annotations.NotNull;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.springframework.stereotype.Component;

@Component("com.etendorx.das.mapper.MELIProductFieldConverterCustom")
public class MELIProductFieldConverterCustom extends MELIProductFieldConverterDefault {

  @Override
  public BigDecimal getPrice(@NotNull Product product) {
    for (ProductPrice productPrice : product.getPricingProductPriceList()) {
      return productPrice.getListPrice();
    }
    return null;
  }
}
