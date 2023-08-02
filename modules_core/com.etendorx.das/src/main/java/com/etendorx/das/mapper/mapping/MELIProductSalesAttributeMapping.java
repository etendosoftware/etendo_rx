package com.etendorx.das.mapper.mapping;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.model.common.plm.Product;
import org.springframework.stereotype.Component;

import com.etendorx.entities.entities.DTOMapping;
import com.etendorx.das.mapper.mapping.beans.MELIProductSaleTerms;

@Component("MELIProduct.salesAttribute")
public class MELIProductSalesAttributeMapping implements DTOMapping<Product, Object> {
  @Override
  public Iterable<MELIProductSaleTerms> map(Product entity) {
    List<MELIProductSaleTerms> result = new ArrayList<>();
    result.add(new MELIProductSaleTerms("WARRANTY_TYPE", "Garantía del vendedor"));
    result.add(new MELIProductSaleTerms("WARRANTY_TIME", "90 días"));
    return result;
  }
}
