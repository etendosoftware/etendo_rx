package com.etendorx.auth.feign.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RxServiceTest {

  @Test
  void testDefaultConstructor() {
    RxService service = new RxService();
    
    assertNotNull(service, "Service should be created");
    assertNull(service.getSecret(), "Secret should be null by default");
    assertNull(service.getSearchkey(), "Searchkey should be null by default");
  }

  @Test
  void testSetAndGetSecret() {
    RxService service = new RxService();
    String secret = "my-secret-key";
    
    service.setSecret(secret);
    
    assertEquals(secret, service.getSecret(), "Secret should match");
  }

  @Test
  void testSetAndGetSearchkey() {
    RxService service = new RxService();
    String searchkey = "das-service";
    
    service.setSearchkey(searchkey);
    
    assertEquals(searchkey, service.getSearchkey(), "Searchkey should match");
  }

  @Test
  void testSetBothFields() {
    RxService service = new RxService();
    String secret = "secret123";
    String searchkey = "service-key";
    
    service.setSecret(secret);
    service.setSearchkey(searchkey);
    
    assertEquals(secret, service.getSecret());
    assertEquals(searchkey, service.getSearchkey());
  }

  @Test
  void testSetNullValues() {
    RxService service = new RxService();
    
    service.setSecret(null);
    service.setSearchkey(null);
    
    assertNull(service.getSecret());
    assertNull(service.getSearchkey());
  }

  @Test
  void testSetEmptyStrings() {
    RxService service = new RxService();
    
    service.setSecret("");
    service.setSearchkey("");
    
    assertEquals("", service.getSecret());
    assertEquals("", service.getSearchkey());
  }

  @Test
  void testSetWithSpecialCharacters() {
    RxService service = new RxService();
    String secret = "secret!@#$%^&*()";
    String searchkey = "key-with-dashes_and_underscores";
    
    service.setSecret(secret);
    service.setSearchkey(searchkey);
    
    assertEquals(secret, service.getSecret());
    assertEquals(searchkey, service.getSearchkey());
  }

  @Test
  void testToString() {
    RxService service = new RxService();
    service.setSecret("test-secret");
    service.setSearchkey("test-key");
    
    String toString = service.toString();
    
    assertNotNull(toString);
    // Lombok @Data generates toString
  }

  @Test
  void testEquals_SameValues() {
    RxService service1 = new RxService();
    service1.setSecret("secret");
    service1.setSearchkey("key");
    
    RxService service2 = new RxService();
    service2.setSecret("secret");
    service2.setSearchkey("key");
    
    assertEquals(service1, service2, "Services with same values should be equal");
  }

  @Test
  void testHashCode_SameValues() {
    RxService service1 = new RxService();
    service1.setSecret("secret");
    service1.setSearchkey("key");
    
    RxService service2 = new RxService();
    service2.setSecret("secret");
    service2.setSearchkey("key");
    
    assertEquals(service1.hashCode(), service2.hashCode(), 
        "Services with same values should have same hashCode");
  }
}
