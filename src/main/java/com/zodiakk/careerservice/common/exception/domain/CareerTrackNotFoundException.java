package com.zodiakk.careerservice.common.exception.domain;

import com.zodiakk.careerservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class CareerTrackNotFoundException extends ResourceNotFoundException {

    public CareerTrackNotFoundException(UUID id) {
        super("Карьерный трек с идентификатором " + id + " не найден");
    }
}
