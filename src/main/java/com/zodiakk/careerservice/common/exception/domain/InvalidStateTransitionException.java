package com.zodiakk.careerservice.common.exception.domain;

import com.zodiakk.careerservice.common.exception.ConflictException;

public class InvalidStateTransitionException extends ConflictException {

    public InvalidStateTransitionException(String entity, String currentState, String targetState) {
        super("%s в состоянии %s не может быть переведён в состояние %s"
                .formatted(entity, currentState, targetState));
    }

    public InvalidStateTransitionException(String message) {
        super(message);
    }
}
