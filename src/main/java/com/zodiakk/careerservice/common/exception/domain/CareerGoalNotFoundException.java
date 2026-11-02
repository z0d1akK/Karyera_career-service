package com.zodiakk.careerservice.common.exception.domain;

import com.zodiakk.careerservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class CareerGoalNotFoundException extends ResourceNotFoundException {

    public CareerGoalNotFoundException(UUID id) {
        super("Карьерная цель с идентификатором " + id + " не найдена");
    }
}
