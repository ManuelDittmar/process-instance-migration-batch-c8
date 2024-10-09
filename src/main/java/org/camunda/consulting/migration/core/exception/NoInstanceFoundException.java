package org.camunda.consulting.migration.core.exception;

public class NoInstanceFoundException extends RuntimeException {
    public NoInstanceFoundException(String message) {
        super(message);
    }
}
