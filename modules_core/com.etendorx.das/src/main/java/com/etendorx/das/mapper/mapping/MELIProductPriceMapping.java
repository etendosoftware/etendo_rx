package com.etendorx.das.mapper.mapping;

import java.math.BigDecimal;
import java.util.Objects;

import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.springframework.stereotype.Component;

import com.etendorx.entities.entities.DTOMapping;

@Component("MELIProduct.price")
public class MELIProductPriceMapping implements DTOMapping<Product, BigDecimal> {
  @Override
  public BigDecimal map(Product entity) {
    if (entity.getPricingProductPriceList() == null) {
      return null;
    }
    return entity.getPricingProductPriceList().stream()
        .filter(Objects::nonNull)
        .map(ProductPrice::getListPrice)
        .findFirst()
        .orElse(null);
  }
}
