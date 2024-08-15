package com.etendorx.asyncprocess.exceptions;

/**
 * This class represents a custom exception for serialization and deserialization (Serdes) operations.
 * It extends the Exception class, thus it is a checked exception.
 */
public class SerdesException extends Exception {

  /**
   * Constructor for the SerdesException class.
   *
   * @param message The detail message. The detail message is saved for later retrieval by the Throwable.getMessage() method.
   * @param cause   The cause (which is saved for later retrieval by the Throwable.getCause() method).
   *                (A null value is permitted, and indicates that the cause is nonexistent or unknown.)
   */
  public SerdesException(String message, Throwable cause) {
    super(message, cause);
  }
}
