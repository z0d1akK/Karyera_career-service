package com.zodiakk.careerservice.common.exception.domain;

import com.zodiakk.careerservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class DevelopmentPlanNotFoundException extends ResourceNotFoundException {

    private DevelopmentPlanNotFoundException(String message) {
        super(message);
    }

    public static DevelopmentPlanNotFoundException plan(UUID id) {
        return new DevelopmentPlanNotFoundException(
                "План развития с идентификатором " + id + " не найден"
        );
    }

    public static DevelopmentPlanNotFoundException activity(UUID id) {
        return new DevelopmentPlanNotFoundException(
                "Мероприятие плана развития с идентификатором " + id + " не найдено"
        );
    }

    public static DevelopmentPlanNotFoundException evidence(UUID id) {
        return new DevelopmentPlanNotFoundException(
                "Подтверждение мероприятия с идентификатором " + id + " не найдено"
        );
    }

    public static DevelopmentPlanNotFoundException review(UUID id) {
        return new DevelopmentPlanNotFoundException(
                "Согласование плана развития с идентификатором " + id + " не найдено"
        );
    }
}
