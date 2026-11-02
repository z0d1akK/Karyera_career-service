package com.zodiakk.careerservice.common.exception.domain;

import com.zodiakk.careerservice.common.exception.ConflictException;

import java.util.UUID;

public class EmptyDevelopmentPlanException extends ConflictException {

    public EmptyDevelopmentPlanException(UUID planId) {
        super("План развития " + planId + " не содержит мероприятий и не может быть утверждён");
    }
}
