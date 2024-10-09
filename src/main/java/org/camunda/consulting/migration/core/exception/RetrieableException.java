package org.camunda.consulting.migration.core.exception;

public class RetrieableException extends RuntimeException {
    public RetrieableException(String message) {
        super(message);
    }
}
