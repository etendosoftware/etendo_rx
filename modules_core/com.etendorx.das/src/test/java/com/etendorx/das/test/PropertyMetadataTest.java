package com.etendorx.das.test;

import com.etendorx.entities.metadata.MetadataUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

@SpringBootTest(properties = {"spring.datasource.url="})
@ComponentScan(basePackages = {"com.etendorx.das", "com.etendorx.entities"})
class PropertyMetadataTest {
  @Autowired
  private MetadataUtil metadataUtil;

  @Test
  void testProductMetadata() {
    var field = metadataUtil.getPropertyMetadata("208", "productCategory");

    Assertions.assertEquals("org.openbravo.model.common.plm.ProductCategory", field.getType());
    Assertions.assertEquals("m_product_category_id", field.getDbColumn());
    Assertions.assertEquals("2012", field.getAdColumnId());
    Assertions.assertEquals("209", field.getAdTableIdRel());
    Assertions.assertFalse(field.isArray());

  }

}
