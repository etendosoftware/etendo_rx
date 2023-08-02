package com.etendorx.das.mapper;

import java.math.BigDecimal;

import org.openbravo.model.common.plm.Product;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.etendorx.entities.entities.DTOMapping;

@Component
public class MELIProductFieldConverter {

  private final DTOMapping<Product, BigDecimal> priceMapping;
  private final DTOMapping<Product, String> currencyIdMapping;
  private final DTOMapping<Product, BigDecimal> availableQuantity;
  private final DTOMapping<Product, Object> salesTerms;
  private final DTOMapping<Product, Object> pictures;
  private final DTOMapping<Product, Object> productAttributes;

  public MELIProductFieldConverter(
      @Qualifier("MELIProduct.price") DTOMapping<Product, BigDecimal> priceMapping,
      @Qualifier("MELIProduct.currencyId") DTOMapping<Product, String> currencyIdMapping,
      @Qualifier("MELIProduct.availableQuantity") DTOMapping<Product, BigDecimal> availableQuantity,
      @Qualifier("MELIProduct.salesTerms") DTOMapping<Product, Object> salesTerms,
      @Qualifier("MELIProduct.pictures") DTOMapping<Product, Object> pictures,
      @Qualifier("MELIProduct.attributes") DTOMapping<Product, Object> productAttributes
  ) {
    super();
    this.priceMapping = priceMapping;
    this.currencyIdMapping = currencyIdMapping;
    this.availableQuantity = availableQuantity;
    this.salesTerms = salesTerms;
    this.pictures = pictures;
    this.productAttributes = productAttributes;
  }

  public String getId(Product product) {
    return product.getId();
  }

  public String getTitle(Product product) {
    return product.getName();
  }

  public String getCategoryId(Product product) {
    return product.getProductCategory().getSearchKey();
  }

  public BigDecimal getPrice(Product product) {
    return priceMapping.map(product);
  }

  public String getCurrencyId(Product product) {
    return currencyIdMapping.map(product);
  }

  public BigDecimal getAvailableQuantity(Product product) {
    return availableQuantity.map(product);
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

  public String getId(MELIProductDTO product) {
    return product.getId();
  }

  public String getTitle(MELIProductDTO product) {
    return product.getTitle();
  }

  public Object getSaleTerms(Product product) {
    return salesTerms.map(product);
  }

  public Object getPictures(Product product) {
    return pictures.map(product);
  }

  public Object getAttributes(Product product) {
    return productAttributes.map(product);
  }

  public Boolean getStocked(Product product) {
    return product.getStocked();
  }

  public Boolean getStocked(MELIProductDTO product) {
    return product.getStocked();
  }


}
