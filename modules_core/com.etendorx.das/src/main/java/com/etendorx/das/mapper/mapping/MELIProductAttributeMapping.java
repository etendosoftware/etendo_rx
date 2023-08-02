package com.etendorx.das.mapper.mapping;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.model.common.plm.Product;
import org.springframework.stereotype.Component;

import com.etendorx.entities.entities.DTOMapping;
import com.etendorx.das.mapper.mapping.beans.MELIProductAttributes;

@Component("MELIProduct.attributes")
public class MELIProductAttributeMapping implements DTOMapping<Product, Object> {
  @Override
  public Iterable<MELIProductAttributes> map(Product entity) {
    List<MELIProductAttributes> result = new ArrayList<>();
    result.add(new MELIProductAttributes("BRAND", "Marca del producto"));
    result.add(new MELIProductAttributes("EAN", "7898095297749"));
    return result;
  }
}
