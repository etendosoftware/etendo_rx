package com.etendorx.gen.commandline;

public class CommandLineException extends RuntimeException {
    public CommandLineException(Exception e) {
        super(e);
    }

    public CommandLineException(String message, Exception e) {
        super(message, e);
    }
}
