package org.camunda.consulting.migration.core.exception;

public class CamundaResourceNotFoundException extends RuntimeException {
    public CamundaResourceNotFoundException(String message) {
        super(message);
    }
}
