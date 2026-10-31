package com.zodiakk.careerservice.common.exception;

public abstract class ResourceNotFoundException extends BusinessException {

    protected ResourceNotFoundException(String message) {
        super(message);
    }
}
