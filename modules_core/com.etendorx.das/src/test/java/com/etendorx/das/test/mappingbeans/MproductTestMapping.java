package com.etendorx.das.test.mappingbeans;

import java.math.BigDecimal;

import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.etendorx.entities.jparepo.ADUserRepository;

@Component()
public class MproductTestMapping {
  @Autowired
  private ADUserRepository adUserRepository;

  public String getCategoryId(Product product) {
    return product.getProductCategory().getSearchKey();
  }

  public BigDecimal getPrice(Product product) {
    BigDecimal price;
    for (ProductPrice productPrice : product.getPricingProductPriceList()) {
      return productPrice.getListPrice();
    }
    return null;
  }

  public String getCurrencyId(Product product) {
    for (ProductPrice productPrice : product.getPricingProductPriceList()) {
      return productPrice.getPriceListVersion().getPriceList().getCurrency().getISOCode();
    }
    return null;
  }

  public BigDecimal getAvailableQuantity(Product product) {
    if(product.getStorageBin() != null) {
      for (StorageDetail storageDetail : product.getStorageBin().getMaterialMgmtStorageDetailList()) {
        return storageDetail.getQuantityOnHand();
      }
    }
    return null;
  }

  public String getBuyingMode(Product product) {
    return "buy_it_now";
  }

  public String getCondition(Product product) {
    return "new";
  }

  public String getListingTypeId(Product product) {
    return "gold_special";
  }

}
