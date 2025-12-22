package com.etendorx.lib.kafka.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AsyncProcessStateTest {

  @Test
  void testEnumValues_ShouldContainAllStates() {
    AsyncProcessState[] states = AsyncProcessState.values();
    
    assertEquals(7, states.length, "Should have exactly 7 states");
  }

  @Test
  void testEnumValue_WAITING() {
    AsyncProcessState state = AsyncProcessState.WAITING;
    
    assertNotNull(state, "WAITING state should exist");
    assertEquals("WAITING", state.name(), "State name should be WAITING");
  }

  @Test
  void testEnumValue_ACCEPTED() {
    AsyncProcessState state = AsyncProcessState.ACCEPTED;
    
    assertNotNull(state, "ACCEPTED state should exist");
    assertEquals("ACCEPTED", state.name(), "State name should be ACCEPTED");
  }

  @Test
  void testEnumValue_DONE() {
    AsyncProcessState state = AsyncProcessState.DONE;
    
    assertNotNull(state, "DONE state should exist");
    assertEquals("DONE", state.name(), "State name should be DONE");
  }

  @Test
  void testEnumValue_REJECTED() {
    AsyncProcessState state = AsyncProcessState.REJECTED;
    
    assertNotNull(state, "REJECTED state should exist");
    assertEquals("REJECTED", state.name(), "State name should be REJECTED");
  }

  @Test
  void testEnumValue_ERROR() {
    AsyncProcessState state = AsyncProcessState.ERROR;
    
    assertNotNull(state, "ERROR state should exist");
    assertEquals("ERROR", state.name(), "State name should be ERROR");
  }

  @Test
  void testEnumValue_STARTED() {
    AsyncProcessState state = AsyncProcessState.STARTED;
    
    assertNotNull(state, "STARTED state should exist");
    assertEquals("STARTED", state.name(), "State name should be STARTED");
  }

  @Test
  void testEnumValue_RETRY() {
    AsyncProcessState state = AsyncProcessState.RETRY;
    
    assertNotNull(state, "RETRY state should exist");
    assertEquals("RETRY", state.name(), "State name should be RETRY");
  }

  @Test
  void testValueOf_WithValidName() {
    AsyncProcessState state = AsyncProcessState.valueOf("WAITING");
    
    assertEquals(AsyncProcessState.WAITING, state, "Should return WAITING state");
  }

  @Test
  void testValueOf_WithInvalidName_ShouldThrowException() {
    assertThrows(IllegalArgumentException.class, () -> {
      AsyncProcessState.valueOf("INVALID_STATE");
    }, "Should throw IllegalArgumentException for invalid state name");
  }

  @Test
  void testValueOf_WithNullName_ShouldThrowException() {
    assertThrows(NullPointerException.class, () -> {
      AsyncProcessState.valueOf(null);
    }, "Should throw NullPointerException for null state name");
  }

  @Test
  void testEnumComparison_WithSameValue() {
    AsyncProcessState state1 = AsyncProcessState.DONE;
    AsyncProcessState state2 = AsyncProcessState.DONE;
    
    assertEquals(state1, state2, "Same enum values should be equal");
    assertSame(state1, state2, "Same enum values should be same instance");
  }

  @Test
  void testEnumComparison_WithDifferentValues() {
    AsyncProcessState state1 = AsyncProcessState.WAITING;
    AsyncProcessState state2 = AsyncProcessState.DONE;
    
    assertNotEquals(state1, state2, "Different enum values should not be equal");
  }

  @Test
  void testEnumInSwitch_WAITING() {
    AsyncProcessState state = AsyncProcessState.WAITING;
    String result = getStateDescription(state);
    
    assertEquals("Process is waiting", result, "Should handle WAITING in switch");
  }

  @Test
  void testEnumInSwitch_DONE() {
    AsyncProcessState state = AsyncProcessState.DONE;
    String result = getStateDescription(state);
    
    assertEquals("Process is complete", result, "Should handle DONE in switch");
  }

  @Test
  void testEnumInSwitch_ERROR() {
    AsyncProcessState state = AsyncProcessState.ERROR;
    String result = getStateDescription(state);
    
    assertEquals("Process encountered an error", result, "Should handle ERROR in switch");
  }

  @Test
  void testEnumOrdinal_Order() {
    assertTrue(AsyncProcessState.WAITING.ordinal() < AsyncProcessState.ACCEPTED.ordinal(),
        "WAITING should come before ACCEPTED");
    assertTrue(AsyncProcessState.ACCEPTED.ordinal() < AsyncProcessState.DONE.ordinal(),
        "ACCEPTED should come before DONE");
  }

  @Test
  void testEnumValues_ContainsAllExpectedStates() {
    AsyncProcessState[] states = AsyncProcessState.values();
    
    assertTrue(contains(states, AsyncProcessState.WAITING));
    assertTrue(contains(states, AsyncProcessState.ACCEPTED));
    assertTrue(contains(states, AsyncProcessState.DONE));
    assertTrue(contains(states, AsyncProcessState.REJECTED));
    assertTrue(contains(states, AsyncProcessState.ERROR));
    assertTrue(contains(states, AsyncProcessState.STARTED));
    assertTrue(contains(states, AsyncProcessState.RETRY));
  }

  @Test
  void testEnumToString() {
    AsyncProcessState state = AsyncProcessState.WAITING;
    
    assertEquals("WAITING", state.toString(), "toString should return state name");
  }

  // Helper method for switch test
  private String getStateDescription(AsyncProcessState state) {
    switch (state) {
      case WAITING:
        return "Process is waiting";
      case ACCEPTED:
        return "Process is accepted";
      case STARTED:
        return "Process has started";
      case DONE:
        return "Process is complete";
      case ERROR:
        return "Process encountered an error";
      case REJECTED:
        return "Process was rejected";
      case RETRY:
        return "Process is retrying";
      default:
        return "Unknown state";
    }
  }

  // Helper method to check if array contains value
  private boolean contains(AsyncProcessState[] states, AsyncProcessState target) {
    for (AsyncProcessState state : states) {
      if (state == target) {
        return true;
      }
    }
    return false;
  }
}
