package com.zodiakk.careerservice.common.exception.domain;

import com.zodiakk.careerservice.common.exception.ConflictException;

public class DuplicateResourceException extends ConflictException {

    private DuplicateResourceException(String message) {
        super(message);
    }

    public static DuplicateResourceException careerTrackCode(String code) {
        return new DuplicateResourceException(
                "Карьерный трек с кодом " + code + " уже существует"
        );
    }

    public static DuplicateResourceException trackNode() {
        return new DuplicateResourceException(
                "Узел с указанными должностью и грейдом уже существует в карьерном треке"
        );
    }

    public static DuplicateResourceException transition() {
        return new DuplicateResourceException(
                "Переход между указанными узлами уже существует"
        );
    }

    public static DuplicateResourceException requiredSkill() {
        return new DuplicateResourceException(
                "Навык уже включён в требования к должности"
        );
    }

    public static DuplicateResourceException reserveCandidate() {
        return new DuplicateResourceException(
                "Сотрудник уже включён в кадровый резерв"
        );
    }
}
