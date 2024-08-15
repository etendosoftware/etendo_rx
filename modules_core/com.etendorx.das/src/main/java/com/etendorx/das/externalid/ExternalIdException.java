package com.etendorx.das.externalid;

public class ExternalIdException extends RuntimeException {

    public ExternalIdException(String message) {
      super(message);
    }

    public ExternalIdException(String message, Throwable cause) {
      super(message, cause);
    }
}
