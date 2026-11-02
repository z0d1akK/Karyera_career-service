package com.zodiakk.careerservice.common.exception.domain;

import com.zodiakk.careerservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class CareerTrackStructureNotFoundException extends ResourceNotFoundException {

    private CareerTrackStructureNotFoundException(String message) {
        super(message);
    }

    public static CareerTrackStructureNotFoundException node(UUID id) {
        return new CareerTrackStructureNotFoundException(
                "Узел карьерного трека с идентификатором " + id + " не найден"
        );
    }

    public static CareerTrackStructureNotFoundException transition(UUID id) {
        return new CareerTrackStructureNotFoundException(
                "Переход карьерного трека с идентификатором " + id + " не найден"
        );
    }
}
