package com.etendorx.utils.auth.key.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * This class is used to throw a {@link ForbiddenException}
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

  /**
   * Constructor for throw a {@link ForbiddenException}
   */
  public ForbiddenException() {
    super();
  }

  /**
   * Constructor for throw a {@link ForbiddenException}
   */
  public ForbiddenException(String message) {
    super(message);
  }
}
