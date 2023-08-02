package com.etendorx.das.mapper;

import org.openbravo.model.common.plm.Product;
import org.springframework.stereotype.Component;

import com.etendorx.entities.entities.DTOConverter;

@Component
public class MELIProductDTOProductConverter implements DTOConverter<Product, MELIProductDTO> {

  private final MELIProductFieldConverter converter;

  public MELIProductDTOProductConverter(
      MELIProductFieldConverter converter) {
    this.converter = converter;
  }

  @Override
  public MELIProductDTO convert(Product product) {
    MELIProductDTO dto = new MELIProductDTO();
    dto.setId(converter.getId(product));
    dto.setTitle(converter.getTitle(product));
    dto.setCategoryId(converter.getCategoryId(product));
    dto.setPrice(converter.getPrice(product));
    dto.setCurrencyId(converter.getCurrencyId(product));
    dto.setAvailableQuantity(converter.getAvailableQuantity(product));
    dto.setBuyingMode(converter.getBuyingMode(product));
    dto.setCondition(converter.getCondition(product));
    dto.setListingTypeId(converter.getListingTypeId(product));
    dto.setSaleTerms(converter.getSaleTerms(product));
    dto.setPictures(converter.getPictures(product));
    dto.setAttributes(converter.getAttributes(product));
    dto.setStocked(converter.getStocked(product));
    return dto;
  }

  @Override
  public Product convert(MELIProductDTO product, Product dto) {
    if (dto == null) {
      dto = new Product();
    }
    dto.setName(converter.getTitle(product));
    dto.setStocked(converter.getStocked(product));
    return dto;
  }

}
