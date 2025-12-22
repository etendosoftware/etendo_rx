package com.etendorx.auth.feign.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ServiceAccessTest {

  @Test
  void testNoArgsConstructor() {
    ServiceAccess serviceAccess = new ServiceAccess();
    
    assertNotNull(serviceAccess, "ServiceAccess should be created");
    assertNull(serviceAccess.getId(), "Id should be null by default");
    assertNull(serviceAccess.getDefaultRoleId(), "DefaultRoleId should be null by default");
    assertNull(serviceAccess.getDefaultOrgId(), "DefaultOrgId should be null by default");
    assertNull(serviceAccess.getRxServiceId(), "RxServiceId should be null by default");
  }

  @Test
  void testSetAndGetId() {
    ServiceAccess serviceAccess = new ServiceAccess();
    String id = "access-123";
    
    serviceAccess.setId(id);
    
    assertEquals(id, serviceAccess.getId(), "Id should match");
  }

  @Test
  void testSetAndGetDefaultRoleId() {
    ServiceAccess serviceAccess = new ServiceAccess();
    String roleId = "role-456";
    
    serviceAccess.setDefaultRoleId(roleId);
    
    assertEquals(roleId, serviceAccess.getDefaultRoleId(), "DefaultRoleId should match");
  }

  @Test
  void testSetAndGetDefaultOrgId() {
    ServiceAccess serviceAccess = new ServiceAccess();
    String orgId = "org-789";
    
    serviceAccess.setDefaultOrgId(orgId);
    
    assertEquals(orgId, serviceAccess.getDefaultOrgId(), "DefaultOrgId should match");
  }

  @Test
  void testSetAndGetRxServiceId() {
    ServiceAccess serviceAccess = new ServiceAccess();
    String serviceId = "service-abc";
    
    serviceAccess.setRxServiceId(serviceId);
    
    assertEquals(serviceId, serviceAccess.getRxServiceId(), "RxServiceId should match");
  }

  @Test
  void testSetAllFields() {
    ServiceAccess serviceAccess = new ServiceAccess();
    String id = "id-1";
    String roleId = "role-2";
    String orgId = "org-3";
    String serviceId = "service-4";
    
    serviceAccess.setId(id);
    serviceAccess.setDefaultRoleId(roleId);
    serviceAccess.setDefaultOrgId(orgId);
    serviceAccess.setRxServiceId(serviceId);
    
    assertEquals(id, serviceAccess.getId());
    assertEquals(roleId, serviceAccess.getDefaultRoleId());
    assertEquals(orgId, serviceAccess.getDefaultOrgId());
    assertEquals(serviceId, serviceAccess.getRxServiceId());
  }

  @Test
  void testSetNullValues() {
    ServiceAccess serviceAccess = new ServiceAccess();
    
    serviceAccess.setId(null);
    serviceAccess.setDefaultRoleId(null);
    serviceAccess.setDefaultOrgId(null);
    serviceAccess.setRxServiceId(null);
    
    assertNull(serviceAccess.getId());
    assertNull(serviceAccess.getDefaultRoleId());
    assertNull(serviceAccess.getDefaultOrgId());
    assertNull(serviceAccess.getRxServiceId());
  }

  @Test
  void testSetEmptyStrings() {
    ServiceAccess serviceAccess = new ServiceAccess();
    
    serviceAccess.setId("");
    serviceAccess.setDefaultRoleId("");
    serviceAccess.setDefaultOrgId("");
    serviceAccess.setRxServiceId("");
    
    assertEquals("", serviceAccess.getId());
    assertEquals("", serviceAccess.getDefaultRoleId());
    assertEquals("", serviceAccess.getDefaultOrgId());
    assertEquals("", serviceAccess.getRxServiceId());
  }

  @Test
  void testSetWithUUIDs() {
    ServiceAccess serviceAccess = new ServiceAccess();
    String uuid1 = "550e8400-e29b-41d4-a716-446655440000";
    String uuid2 = "6ba7b810-9dad-11d1-80b4-00c04fd430c8";
    String uuid3 = "6ba7b811-9dad-11d1-80b4-00c04fd430c8";
    String uuid4 = "6ba7b812-9dad-11d1-80b4-00c04fd430c8";
    
    serviceAccess.setId(uuid1);
    serviceAccess.setDefaultRoleId(uuid2);
    serviceAccess.setDefaultOrgId(uuid3);
    serviceAccess.setRxServiceId(uuid4);
    
    assertEquals(uuid1, serviceAccess.getId());
    assertEquals(uuid2, serviceAccess.getDefaultRoleId());
    assertEquals(uuid3, serviceAccess.getDefaultOrgId());
    assertEquals(uuid4, serviceAccess.getRxServiceId());
  }

  @Test
  void testToString() {
    ServiceAccess serviceAccess = new ServiceAccess();
    serviceAccess.setId("test-id");
    serviceAccess.setDefaultRoleId("test-role");
    
    String toString = serviceAccess.toString();
    
    assertNotNull(toString);
    // Lombok @Data generates toString
  }

  @Test
  void testEquals_SameValues() {
    ServiceAccess access1 = new ServiceAccess();
    access1.setId("id");
    access1.setDefaultRoleId("role");
    access1.setDefaultOrgId("org");
    access1.setRxServiceId("service");
    
    ServiceAccess access2 = new ServiceAccess();
    access2.setId("id");
    access2.setDefaultRoleId("role");
    access2.setDefaultOrgId("org");
    access2.setRxServiceId("service");
    
    assertEquals(access1, access2, "ServiceAccess with same values should be equal");
  }

  @Test
  void testHashCode_SameValues() {
    ServiceAccess access1 = new ServiceAccess();
    access1.setId("id");
    access1.setDefaultRoleId("role");
    
    ServiceAccess access2 = new ServiceAccess();
    access2.setId("id");
    access2.setDefaultRoleId("role");
    
    assertEquals(access1.hashCode(), access2.hashCode(), 
        "ServiceAccess with same values should have same hashCode");
  }

  @Test
  void testMultipleInstances_AreIndependent() {
    ServiceAccess access1 = new ServiceAccess();
    access1.setId("id1");
    
    ServiceAccess access2 = new ServiceAccess();
    access2.setId("id2");
    
    assertNotEquals(access1.getId(), access2.getId());
  }
}
