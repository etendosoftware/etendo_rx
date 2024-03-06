/*
 * Copyright 2022-2024  Futit Services SL
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.etendorx.das.integration;

import com.etendorx.entities.metadata.MetadataUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

/**
 * This class contains integration tests for the MetadataUtil class.
 * It uses Spring Boot's testing support to load the application context and autowire the MetadataUtil bean.
 * The tests verify the behavior of the getPropertyMetadata method when it is called with valid arguments.
 */
@SpringBootTest(properties = {"spring.datasource.url="})
@ComponentScan(basePackages = {"com.etendorx.das", "com.etendorx.entities"})
public class PropertyMetadataTest {

  /**
   * The MetadataUtil instance to be tested.
   */
  @Autowired
  private MetadataUtil metadataUtil;

  /**
   * This test method verifies the behavior of the getPropertyMetadata method in the MetadataUtil class
   * when it is called with the arguments "208" and "productCategory".
   *
   * The getPropertyMetadata method is expected to return a Field object with the following properties:
   * - type: "org.openbravo.model.common.plm.ProductCategory",
   * - dbColumn: "m_product_category_id",
   * - adColumnId: "2012",
   * - adTableIdRel: "209",
   * - array: false.
   *
   * The returned Field object is then compared to these expected properties to verify that the method
   * has returned the correct Field.
   */
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
