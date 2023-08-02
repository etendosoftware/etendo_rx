package com.etendorx.das.mapper.mapping;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.model.common.plm.Product;
import org.springframework.stereotype.Component;

import com.etendorx.entities.entities.DTOMapping;
import com.etendorx.das.mapper.mapping.beans.MELIProductPictures;

@Component("MELIProduct.pictures")
public class MELIProductPicturesMapping implements DTOMapping<Product, Object> {
  @Override
  public Iterable<MELIProductPictures> map(Product entity) {
    List<MELIProductPictures> result = new ArrayList<>();
    result.add(new MELIProductPictures("http://mla-s2-p.mlstatic.com/968521-MLA20805195516_072016-O.jpg"));
    return result;
  }
}
