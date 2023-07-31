package com.etendorx.das.mapper;

import java.util.ArrayList;
import java.util.List;

import org.openbravo.model.common.plm.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.etendorx.entities.jparepo.ProductRepository;

@Component
public class ProductDASRepositoryImpl implements ProductDASRepository {

  private final ProductRepository productRepository;
  private final MELIProductDTOConverter converter;

  public ProductDASRepositoryImpl(ProductRepository productRepository,
      @Autowired @Qualifier("com.etendorx.das.mapper.MELIProductFieldConverterCustom") MELIProductDTOConverter converter) {
    this.productRepository = productRepository;
    this.converter = converter;
  }

  @Override
  public Iterable<MELIProductDTO> findAll() {
    List<MELIProductDTO> dtos = new ArrayList<>();
    Iterable<Product> products = productRepository.findAll();
    for (Product product : products) {
      dtos.add(converter.convertDTO(product));
    }
    return null;
  }

  public MELIProductDTO findById(String id) {
    var product = productRepository.findById(id);
    if(product.isPresent()) {
      return converter.convertDTO(product.get());
    } else {
      return null;
    }
  }

  @Override
  public MELIProductDTO save(MELIProductDTO mELIproduct) {
    var product = converter.convertDTO(mELIproduct, null);
    productRepository.save(product);
    return converter.convertDTO(product);
  }

  @Override
  public MELIProductDTO updated(MELIProductDTO meliProduct) {
    var product = productRepository.findById(meliProduct.getId());
    productRepository.save(converter.convertDTO(meliProduct, product.get()));
    return converter.convertDTO(product.get());
  }
}
