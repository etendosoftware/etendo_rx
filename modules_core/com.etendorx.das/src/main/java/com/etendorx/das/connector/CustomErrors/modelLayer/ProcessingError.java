package com.etendorx.das.connector.CustomErrors.modelLayer;

public class ProcessingError extends Throwable {
    public ProcessingError(String message, String eMessage) {
            super(message);
        }
}
