package com.etendorx.das.mapper;

import java.math.BigDecimal;

import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("com.etendorx.das.mapper.MELIProductFieldConverterDefault")
public class MELIProductFieldConverterDefault implements MELIProductFieldConverter {

  public MELIProductFieldConverterDefault() {
    super();
  }

  @Override
  public String getId(Product product) {
    return product.getId();
  }

  @Override
  public String getTitle(Product product) {
    return product.getName();
  }

  @Override
  public String getCategoryId(Product product) {
    return product.getProductCategory().getSearchKey();
  }

  @Override
  public BigDecimal getPrice(Product product) {
    return null;
  }

  @Override
  public String getCurrencyId(Product product) {
    for (ProductPrice productPrice : product.getPricingProductPriceList()) {
      return productPrice.getPriceListVersion().getPriceList().getCurrency().getISOCode();
    }
    return null;
  }

  @Override
  public BigDecimal getAvailableQuantity(Product product) {
    if(product.getStorageBin() != null) {
      for (StorageDetail storageDetail : product.getStorageBin().getMaterialMgmtStorageDetailList()) {
        return storageDetail.getQuantityOnHand();
      }
    }
    return null;
  }

  @Override
  public String getBuyingMode(Product product) {
    return "buy_it_now";
  }

  @Override
  public String getCondition(Product product) {
    return "new";
  }

  @Override
  public String getListingTypeId(Product product) {
    return "gold_special";
  }

  @Override
  public String getId(MELIProductDTO product) {
    return product.getId();
  }

  @Override
  public String getTitle(MELIProductDTO product) {
    return product.getTitle();
  }

}
