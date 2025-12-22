package com.etendorx.clientrest.base;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientRestConstantsTest {

  @Test
  void testDasAuthMethodGlobal_HasCorrectValue() {
    assertEquals("global", ClientRestConstants.DAS_AUTH_METHOD_GLOBAL, 
        "DAS_AUTH_METHOD_GLOBAL should be 'global'");
  }

  @Test
  void testDasAuthMethodGlobal_IsNotNull() {
    assertNotNull(ClientRestConstants.DAS_AUTH_METHOD_GLOBAL, 
        "DAS_AUTH_METHOD_GLOBAL should not be null");
  }

  @Test
  void testDasAuthMethodGlobal_IsNotEmpty() {
    assertFalse(ClientRestConstants.DAS_AUTH_METHOD_GLOBAL.isEmpty(), 
        "DAS_AUTH_METHOD_GLOBAL should not be empty");
  }

  @Test
  void testXToken_HasCorrectValue() throws Exception {
    // X_TOKEN is package-private, so we use reflection to test it
    java.lang.reflect.Field xTokenField = ClientRestConstants.class.getDeclaredField("X_TOKEN");
    xTokenField.setAccessible(true);
    String xTokenValue = (String) xTokenField.get(null);
    
    assertEquals("X-TOKEN", xTokenValue, "X_TOKEN should be 'X-TOKEN'");
  }

  @Test
  void testXToken_IsNotNull() throws Exception {
    java.lang.reflect.Field xTokenField = ClientRestConstants.class.getDeclaredField("X_TOKEN");
    xTokenField.setAccessible(true);
    String xTokenValue = (String) xTokenField.get(null);
    
    assertNotNull(xTokenValue, "X_TOKEN should not be null");
  }

  @Test
  void testConstants_AreFinal() throws Exception {
    java.lang.reflect.Field dasAuthField = ClientRestConstants.class.getDeclaredField("DAS_AUTH_METHOD_GLOBAL");
    
    assertTrue(java.lang.reflect.Modifier.isFinal(dasAuthField.getModifiers()), 
        "DAS_AUTH_METHOD_GLOBAL should be final");
  }

  @Test
  void testConstants_AreStatic() throws Exception {
    java.lang.reflect.Field dasAuthField = ClientRestConstants.class.getDeclaredField("DAS_AUTH_METHOD_GLOBAL");
    
    assertTrue(java.lang.reflect.Modifier.isStatic(dasAuthField.getModifiers()), 
        "DAS_AUTH_METHOD_GLOBAL should be static");
  }

  @Test
  void testDasAuthMethodGlobal_CanBeUsedInComparisons() {
    String testValue = "global";
    
    assertEquals(ClientRestConstants.DAS_AUTH_METHOD_GLOBAL, testValue, 
        "Should be equal to 'global'");
    assertTrue(ClientRestConstants.DAS_AUTH_METHOD_GLOBAL.equals(testValue), 
        "equals() should return true for 'global'");
  }

  @Test
  void testDasAuthMethodGlobal_IsCaseSensitive() {
    assertNotEquals("GLOBAL", ClientRestConstants.DAS_AUTH_METHOD_GLOBAL, 
        "Should be case-sensitive");
    assertNotEquals("Global", ClientRestConstants.DAS_AUTH_METHOD_GLOBAL, 
        "Should be case-sensitive");
  }

  @Test
  void testClientRestConstants_CanBeInstantiated() {
    // Even though it's a constants class, it should be instantiable unless explicitly prevented
    assertDoesNotThrow(() -> {
      new ClientRestConstants();
    }, "Should be able to instantiate ClientRestConstants");
  }

  @Test
  void testDasAuthMethodGlobal_Length() {
    assertEquals(6, ClientRestConstants.DAS_AUTH_METHOD_GLOBAL.length(), 
        "DAS_AUTH_METHOD_GLOBAL length should be 6");
  }

  @Test
  void testDasAuthMethodGlobal_NoWhitespace() {
    assertFalse(ClientRestConstants.DAS_AUTH_METHOD_GLOBAL.contains(" "), 
        "Should not contain whitespace");
    assertEquals(ClientRestConstants.DAS_AUTH_METHOD_GLOBAL.trim(), 
        ClientRestConstants.DAS_AUTH_METHOD_GLOBAL, 
        "Should not have leading or trailing whitespace");
  }
}
