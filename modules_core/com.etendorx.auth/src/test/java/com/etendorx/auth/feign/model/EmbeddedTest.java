package com.etendorx.auth.feign.model;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EmbeddedTest {

  @Test
  void testDefaultConstructor() {
    Embedded embedded = new Embedded();
    
    assertNotNull(embedded, "Embedded should be created");
    assertNull(embedded.getRxServiceses(), "RxServiceses should be null by default");
  }

  @Test
  void testSetAndGetRxServiceses() {
    Embedded embedded = new Embedded();
    RxService service1 = new RxService();
    service1.setSearchkey("service1");
    RxService service2 = new RxService();
    service2.setSearchkey("service2");
    List<RxService> services = Arrays.asList(service1, service2);
    
    embedded.setRxServiceses(services);
    
    assertEquals(services, embedded.getRxServiceses());
    assertEquals(2, embedded.getRxServiceses().size());
  }

  @Test
  void testSetRxServiceses_WithEmptyList() {
    Embedded embedded = new Embedded();
    List<RxService> emptyList = Collections.emptyList();
    
    embedded.setRxServiceses(emptyList);
    
    assertNotNull(embedded.getRxServiceses());
    assertTrue(embedded.getRxServiceses().isEmpty());
  }

  @Test
  void testSetRxServiceses_WithNull() {
    Embedded embedded = new Embedded();
    
    embedded.setRxServiceses(null);
    
    assertNull(embedded.getRxServiceses());
  }

  @Test
  void testSetRxServiceses_WithSingleService() {
    Embedded embedded = new Embedded();
    RxService service = new RxService();
    service.setSearchkey("single-service");
    service.setSecret("secret-key");
    List<RxService> services = Collections.singletonList(service);
    
    embedded.setRxServiceses(services);
    
    assertEquals(1, embedded.getRxServiceses().size());
    assertEquals("single-service", embedded.getRxServiceses().get(0).getSearchkey());
  }

  @Test
  void testSetRxServiceses_WithMultipleServices() {
    Embedded embedded = new Embedded();
    RxService service1 = new RxService();
    service1.setSearchkey("das");
    service1.setSecret("secret1");
    
    RxService service2 = new RxService();
    service2.setSearchkey("auth");
    service2.setSecret("secret2");
    
    RxService service3 = new RxService();
    service3.setSearchkey("edge");
    service3.setSecret("secret3");
    
    List<RxService> services = Arrays.asList(service1, service2, service3);
    
    embedded.setRxServiceses(services);
    
    assertEquals(3, embedded.getRxServiceses().size());
    assertEquals("das", embedded.getRxServiceses().get(0).getSearchkey());
    assertEquals("auth", embedded.getRxServiceses().get(1).getSearchkey());
    assertEquals("edge", embedded.getRxServiceses().get(2).getSearchkey());
  }

  @Test
  void testEquals_SameValues() {
    Embedded embedded1 = new Embedded();
    RxService service = new RxService();
    service.setSearchkey("service");
    embedded1.setRxServiceses(Collections.singletonList(service));
    
    Embedded embedded2 = new Embedded();
    RxService service2 = new RxService();
    service2.setSearchkey("service");
    embedded2.setRxServiceses(Collections.singletonList(service2));
    
    assertEquals(embedded1, embedded2);
  }

  @Test
  void testHashCode_SameValues() {
    Embedded embedded1 = new Embedded();
    RxService service = new RxService();
    service.setSearchkey("service");
    embedded1.setRxServiceses(Collections.singletonList(service));
    
    Embedded embedded2 = new Embedded();
    RxService service2 = new RxService();
    service2.setSearchkey("service");
    embedded2.setRxServiceses(Collections.singletonList(service2));
    
    assertEquals(embedded1.hashCode(), embedded2.hashCode());
  }

  @Test
  void testToString() {
    Embedded embedded = new Embedded();
    RxService service = new RxService();
    service.setSearchkey("test-service");
    embedded.setRxServiceses(Collections.singletonList(service));
    
    String toString = embedded.toString();
    
    assertNotNull(toString);
  }

  @Test
  void testSetRxServiceses_ReplacesExistingList() {
    Embedded embedded = new Embedded();
    
    RxService service1 = new RxService();
    service1.setSearchkey("service1");
    embedded.setRxServiceses(Collections.singletonList(service1));
    assertEquals(1, embedded.getRxServiceses().size());
    
    RxService service2 = new RxService();
    service2.setSearchkey("service2");
    RxService service3 = new RxService();
    service3.setSearchkey("service3");
    embedded.setRxServiceses(Arrays.asList(service2, service3));
    
    assertEquals(2, embedded.getRxServiceses().size());
    assertEquals("service2", embedded.getRxServiceses().get(0).getSearchkey());
  }

  @Test
  void testMultipleInstances_AreIndependent() {
    Embedded embedded1 = new Embedded();
    RxService service1 = new RxService();
    service1.setSearchkey("service1");
    embedded1.setRxServiceses(Collections.singletonList(service1));
    
    Embedded embedded2 = new Embedded();
    RxService service2 = new RxService();
    service2.setSearchkey("service2");
    embedded2.setRxServiceses(Collections.singletonList(service2));
    
    assertNotEquals(embedded1.getRxServiceses().get(0).getSearchkey(),
                    embedded2.getRxServiceses().get(0).getSearchkey());
  }
}
