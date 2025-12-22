package com.etendorx.auth.feign.model;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class UserModelTest {

  @Test
  void testNoArgsConstructor() {
    UserModel user = new UserModel();
    
    assertNotNull(user, "User should be created");
    assertNull(user.getId());
    assertNull(user.getActive());
    assertNull(user.getClient());
    assertNull(user.getOrganization());
    assertNull(user.getDefaultClient());
    assertNull(user.getDefaultOrganization());
    assertNull(user.getUsername());
    assertNull(user.getPassword());
    assertNull(user.geteTRXRxServicesAccessList());
  }

  @Test
  void testSetAndGetId() {
    UserModel user = new UserModel();
    String id = "user-123";
    
    user.setId(id);
    
    assertEquals(id, user.getId());
  }

  @Test
  void testSetAndGetActive() {
    UserModel user = new UserModel();
    
    user.setActive(true);
    assertTrue(user.getActive());
    
    user.setActive(false);
    assertFalse(user.getActive());
  }

  @Test
  void testSetAndGetClient() {
    UserModel user = new UserModel();
    String client = "client-456";
    
    user.setClient(client);
    
    assertEquals(client, user.getClient());
  }

  @Test
  void testSetAndGetOrganization() {
    UserModel user = new UserModel();
    String org = "org-789";
    
    user.setOrganization(org);
    
    assertEquals(org, user.getOrganization());
  }

  @Test
  void testSetAndGetDefaultClient() {
    UserModel user = new UserModel();
    String defaultClient = "default-client-123";
    
    user.setDefaultClient(defaultClient);
    
    assertEquals(defaultClient, user.getDefaultClient());
  }

  @Test
  void testSetAndGetDefaultOrganization() {
    UserModel user = new UserModel();
    String defaultOrg = "default-org-456";
    
    user.setDefaultOrganization(defaultOrg);
    
    assertEquals(defaultOrg, user.getDefaultOrganization());
  }

  @Test
  void testSetAndGetUsername() {
    UserModel user = new UserModel();
    String username = "john.doe";
    
    user.setUsername(username);
    
    assertEquals(username, user.getUsername());
  }

  @Test
  void testSetAndGetPassword() {
    UserModel user = new UserModel();
    String password = "securePassword123";
    
    user.setPassword(password);
    
    assertEquals(password, user.getPassword());
  }

  @Test
  void testSetAndGetServiceAccessList() {
    UserModel user = new UserModel();
    ServiceAccess access1 = new ServiceAccess();
    access1.setId("access-1");
    ServiceAccess access2 = new ServiceAccess();
    access2.setId("access-2");
    List<ServiceAccess> accessList = Arrays.asList(access1, access2);
    
    user.seteTRXRxServicesAccessList(accessList);
    
    assertEquals(accessList, user.geteTRXRxServicesAccessList());
    assertEquals(2, user.geteTRXRxServicesAccessList().size());
  }

  @Test
  void testSetServiceAccessList_WithEmptyList() {
    UserModel user = new UserModel();
    List<ServiceAccess> emptyList = Collections.emptyList();
    
    user.seteTRXRxServicesAccessList(emptyList);
    
    assertNotNull(user.geteTRXRxServicesAccessList());
    assertTrue(user.geteTRXRxServicesAccessList().isEmpty());
  }

  @Test
  void testSetAllFields() {
    UserModel user = new UserModel();
    
    user.setId("id-1");
    user.setActive(true);
    user.setClient("client-1");
    user.setOrganization("org-1");
    user.setDefaultClient("default-client-1");
    user.setDefaultOrganization("default-org-1");
    user.setUsername("user1");
    user.setPassword("pass1");
    user.seteTRXRxServicesAccessList(Collections.emptyList());
    
    assertEquals("id-1", user.getId());
    assertTrue(user.getActive());
    assertEquals("client-1", user.getClient());
    assertEquals("org-1", user.getOrganization());
    assertEquals("default-client-1", user.getDefaultClient());
    assertEquals("default-org-1", user.getDefaultOrganization());
    assertEquals("user1", user.getUsername());
    assertEquals("pass1", user.getPassword());
    assertNotNull(user.geteTRXRxServicesAccessList());
  }

  @Test
  void testSetNullValues() {
    UserModel user = new UserModel();
    
    user.setId(null);
    user.setActive(null);
    user.setClient(null);
    user.setOrganization(null);
    user.setDefaultClient(null);
    user.setDefaultOrganization(null);
    user.setUsername(null);
    user.setPassword(null);
    user.seteTRXRxServicesAccessList(null);
    
    assertNull(user.getId());
    assertNull(user.getActive());
    assertNull(user.getClient());
    assertNull(user.getOrganization());
    assertNull(user.getDefaultClient());
    assertNull(user.getDefaultOrganization());
    assertNull(user.getUsername());
    assertNull(user.getPassword());
    assertNull(user.geteTRXRxServicesAccessList());
  }

  @Test
  void testEquals_SameValues() {
    UserModel user1 = new UserModel();
    user1.setId("id");
    user1.setUsername("user");
    
    UserModel user2 = new UserModel();
    user2.setId("id");
    user2.setUsername("user");
    
    assertEquals(user1, user2);
  }

  @Test
  void testHashCode_SameValues() {
    UserModel user1 = new UserModel();
    user1.setId("id");
    user1.setUsername("user");
    
    UserModel user2 = new UserModel();
    user2.setId("id");
    user2.setUsername("user");
    
    assertEquals(user1.hashCode(), user2.hashCode());
  }

  @Test
  void testToString() {
    UserModel user = new UserModel();
    user.setId("test-id");
    user.setUsername("test-user");
    
    String toString = user.toString();
    
    assertNotNull(toString);
  }

  @Test
  void testSetUsername_WithEmail() {
    UserModel user = new UserModel();
    String email = "user@example.com";
    
    user.setUsername(email);
    
    assertEquals(email, user.getUsername());
  }

  @Test
  void testMultipleInstances_AreIndependent() {
    UserModel user1 = new UserModel();
    user1.setId("user1");
    
    UserModel user2 = new UserModel();
    user2.setId("user2");
    
    assertNotEquals(user1.getId(), user2.getId());
  }
}
