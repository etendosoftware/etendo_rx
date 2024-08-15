package com.etendorx.das.test.mappingbeans;

import com.etendorx.entities.jparepo.ADUserRepository;
import org.openbravo.model.common.currency.Currency;
import org.openbravo.model.common.plm.Product;
import org.openbravo.model.materialmgmt.onhandquantity.StorageDetail;
import org.openbravo.model.pricing.pricelist.PriceList;
import org.openbravo.model.pricing.pricelist.PriceListVersion;
import org.openbravo.model.pricing.pricelist.ProductPrice;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component()
public class MproductTestMapping {
  public static final String BUYING_MODE = "buy_it_now";
  public static final String CONDITION_NEW = "new";
  public static final String LISTING_TYPE_GOLD = "gold_special";
  @Autowired
  private ADUserRepository adUserRepository;

  public String getCategoryId(Product product) {
    return product.getProductCategory().getSearchKey();
  }

  public BigDecimal getPrice(Product product) {
    return product.getPricingProductPriceList()
        .stream()
        .findFirst()
        .map(ProductPrice::getListPrice)
        .orElse(null);
  }

  public String getCurrencyId(Product product) {
    return product.getPricingProductPriceList()
        .stream()
        .findFirst()
        .map(ProductPrice::getPriceListVersion)
        .map(PriceListVersion::getPriceList)
        .map(PriceList::getCurrency)
        .map(Currency::getISOCode)
        .orElse(null);
  }

  public BigDecimal getAvailableQuantity(Product product) {
    return Objects.requireNonNull(product)
        .getStorageBin()
        .getMaterialMgmtStorageDetailList()
        .stream()
        .findFirst()
        .map(StorageDetail::getQuantityOnHand)
        .orElse(null);
  }

  public String getBuyingMode(Product product) { // NOSONAR
    return BUYING_MODE;
  }

  public String getCondition(Product product) { // NOSONAR
    return CONDITION_NEW;
  }

  public String getListingTypeId(Product product) { // NOSONAR
    return LISTING_TYPE_GOLD;
  }

}
