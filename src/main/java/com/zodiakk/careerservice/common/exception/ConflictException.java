package com.zodiakk.careerservice.common.exception;

public abstract class ConflictException extends BusinessException {

    protected ConflictException(String message) {
        super(message);
    }
}
