package com.etendorx.das.mapper.mapping;

import java.util.Objects;

import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.springframework.stereotype.Component;

import com.etendorx.entities.entities.DTOMapping;

@Component("MELIProduct.currencyId")
public class MELIProductCurrencyIdMapping implements DTOMapping<Product, String> {

  @Override
  public String map(Product entity) {
    if (entity == null) {
      return null;
    }

    return entity.getPricingProductPriceList().stream()
        .filter(Objects::nonNull)
        .map(ProductPrice::getPriceListVersion)
        .filter(Objects::nonNull)
        .map(PriceListVersion::getPriceList)
        .filter(Objects::nonNull)
        .map(PriceList::getCurrency)
        .filter(Objects::nonNull)
        .map(Currency::getISOCode)
        .findFirst()
        .orElse(null);
  }


}
