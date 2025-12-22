package com.etendorx.auth.feign.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServicesAccessModelTest {

  @Test
  void testNoArgsConstructor() {
    ServicesAccessModel model = new ServicesAccessModel();
    
    assertNotNull(model, "Model should be created");
    assertNull(model.get_embedded(), "Embedded should be null by default");
  }

  @Test
  void testSetAndGetEmbedded() {
    ServicesAccessModel model = new ServicesAccessModel();
    Embedded embedded = new Embedded();
    
    model.set_embedded(embedded);
    
    assertEquals(embedded, model.get_embedded(), "Embedded should match");
  }

  @Test
  void testSetEmbedded_WithNull() {
    ServicesAccessModel model = new ServicesAccessModel();
    
    model.set_embedded(null);
    
    assertNull(model.get_embedded(), "Embedded should be null");
  }

  @Test
  void testSetEmbedded_WithPopulatedEmbedded() {
    ServicesAccessModel model = new ServicesAccessModel();
    Embedded embedded = new Embedded();
    RxService service = new RxService();
    service.setSearchkey("test-service");
    embedded.setRxServiceses(java.util.Collections.singletonList(service));
    
    model.set_embedded(embedded);
    
    assertNotNull(model.get_embedded());
    assertNotNull(model.get_embedded().getRxServiceses());
    assertEquals(1, model.get_embedded().getRxServiceses().size());
  }

  @Test
  void testEquals_SameValues() {
    ServicesAccessModel model1 = new ServicesAccessModel();
    Embedded embedded1 = new Embedded();
    model1.set_embedded(embedded1);
    
    ServicesAccessModel model2 = new ServicesAccessModel();
    Embedded embedded2 = new Embedded();
    model2.set_embedded(embedded2);
    
    assertEquals(model1, model2);
  }

  @Test
  void testHashCode_SameValues() {
    ServicesAccessModel model1 = new ServicesAccessModel();
    Embedded embedded1 = new Embedded();
    model1.set_embedded(embedded1);
    
    ServicesAccessModel model2 = new ServicesAccessModel();
    Embedded embedded2 = new Embedded();
    model2.set_embedded(embedded2);
    
    assertEquals(model1.hashCode(), model2.hashCode());
  }

  @Test
  void testToString() {
    ServicesAccessModel model = new ServicesAccessModel();
    Embedded embedded = new Embedded();
    model.set_embedded(embedded);
    
    String toString = model.toString();
    
    assertNotNull(toString);
  }

  @Test
  void testSetEmbedded_ReplacesExistingValue() {
    ServicesAccessModel model = new ServicesAccessModel();
    
    Embedded embedded1 = new Embedded();
    model.set_embedded(embedded1);
    assertEquals(embedded1, model.get_embedded());
    
    Embedded embedded2 = new Embedded();
    model.set_embedded(embedded2);
    assertEquals(embedded2, model.get_embedded());
    assertNotEquals(embedded1, embedded2);
  }

  @Test
  void testMultipleInstances_AreIndependent() {
    ServicesAccessModel model1 = new ServicesAccessModel();
    Embedded embedded1 = new Embedded();
    RxService service1 = new RxService();
    service1.setSearchkey("service1");
    embedded1.setRxServiceses(java.util.Collections.singletonList(service1));
    model1.set_embedded(embedded1);
    
    ServicesAccessModel model2 = new ServicesAccessModel();
    
    assertNotNull(model1.get_embedded());
    assertNull(model2.get_embedded());
  }
}
