package com.etendorx.das.mapper.mapping;

import java.math.BigDecimal;

import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.springframework.stereotype.Component;

import com.etendorx.entities.entities.DTOMapping;

@Component("MELIProduct.availableQuantity")
public class MELIProductAvailableQuantityMapping implements DTOMapping<Product, BigDecimal> {

  @Override
  public BigDecimal map(Product entity) {
    if(entity.getStorageBin() == null || entity.getStorageBin().getMaterialMgmtStorageDetailList() == null) {
      return null;
    }
    return entity.getStorageBin().getMaterialMgmtStorageDetailList().stream()
        .map(StorageDetail::getQuantityOnHand)
        .findFirst()
        .orElse(null);
  }
}
