package com.zodiakk.careerservice.common.exception;

public class IntegrationUnavailableException extends RuntimeException {

    public IntegrationUnavailableException(String message) {
        super(message);
    }

    public IntegrationUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
