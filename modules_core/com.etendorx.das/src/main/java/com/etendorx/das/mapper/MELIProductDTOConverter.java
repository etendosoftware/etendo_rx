package com.etendorx.das.mapper;

import java.math.BigDecimal;

import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.springframework.stereotype.Component;

import lombok.extern.apachecommons.CommonsLog;

@Component
public class MELIProductDTOConverter  {

  private final MELIProductFieldConverter converter;

  public MELIProductDTOConverter(MELIProductFieldConverter converter) {
    this.converter = converter;
  }

  MELIProductDTO convertDTO(Product product) {
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
    return dto;
  }

  Product convertDTO(MELIProductDTO product, Product dto) {
    if(dto == null) {
      dto = new Product();
    }
    dto.setName(converter.getTitle(product));
    return dto;
  }

}
