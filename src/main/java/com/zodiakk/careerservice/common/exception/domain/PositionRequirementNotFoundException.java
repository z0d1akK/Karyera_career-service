package com.zodiakk.careerservice.common.exception.domain;

import com.zodiakk.careerservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class PositionRequirementNotFoundException extends ResourceNotFoundException {

    private PositionRequirementNotFoundException(String message) {
        super(message);
    }

    public static PositionRequirementNotFoundException requirement(UUID id) {
        return new PositionRequirementNotFoundException(
                "Требования к должности с идентификатором " + id + " не найдены"
        );
    }

    public static PositionRequirementNotFoundException of(String message) {
        return new PositionRequirementNotFoundException(message);
    }

    public static PositionRequirementNotFoundException skill(UUID id) {
        return new PositionRequirementNotFoundException(
                "Требование к навыку с идентификатором " + id + " не найдено"
        );
    }
}
