package com.etendo.test;

public class BrokenExample {

  public void doSomething(boolean condition) {
    String name = "Etendo";
    int count = 5;

    // Violación de UseStringBuilderInsteadOfConcat
    String message = "Name: " + name + ", Count: " + count;

    if (condition) {
      // Violación de UseADMessageForExceptions
      throw new IllegalArgumentException("This is a hardcoded exception message");
    }
  }
}
