package com.zodiakk.careerservice.common.exception.domain;

import com.zodiakk.careerservice.common.exception.ConflictException;

public class CyclicCareerTrackException extends ConflictException {

    public CyclicCareerTrackException() {
        super("Карьерный трек содержит цикл в переходах между узлами");
    }
}
