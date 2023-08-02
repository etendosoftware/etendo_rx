package com.etendorx.das.mapper;

import org.openbravo.model.common.plm.Product;
import org.springframework.stereotype.Component;

import com.etendorx.entities.entities.BaseDTORepositoryDefault;
import com.etendorx.entities.jparepo.ProductRepository;

@Component("MELI.ProductDASRepositoryDefault")
public class MELIProductDTORepositoryDefault extends BaseDTORepositoryDefault<Product, MELIProductDTO> {

  public MELIProductDTORepositoryDefault(ProductRepository productRepository,
      MELIProductDTOProductConverter converter) {
    super(productRepository, converter);
  }

}
