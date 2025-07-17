package com.etendorx.auth.exception;

/**
 * Exception used to represent OAuth-related errors that occur during authentication or token handling processes.
 * <p>
 * This is a custom runtime exception that encapsulates low-level exceptions or invalid conditions specific
 * to the OAuth flow, such as decoding issues, unexpected parameter values, or internal processing errors.
 * </p>
 *
 * <p>Typical use cases include:</p>
 * <ul>
 *   <li>Invalid or malformed `state` parameters</li>
 *   <li>Errors in URI parsing related to OAuth redirects</li>
 *   <li>General unexpected OAuth flow failures</li>
 * </ul>
 *
 * <p>It is unchecked (extends {@link RuntimeException}) to avoid cluttering method signatures.</p>
 */
public class OAuthException extends RuntimeException {

  /**
   * Constructs a new OAuthException with the specified detail message.
   *
   * @param message the detail message describing the exception.
   */
  public OAuthException(String message) {
    super(message);
  }

  /**
   * Constructs a new OAuthException with the specified detail message and cause.
   *
   * @param message the detail message describing the exception.
   * @param cause   the cause of the exception, useful for exception chaining.
   */
  public OAuthException(String message, Throwable cause) {
    super(message, cause);
  }
}
