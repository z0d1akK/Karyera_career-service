package com.zodiakk.careerservice.common.exception.domain;

import com.zodiakk.careerservice.common.exception.ResourceNotFoundException;

import java.util.UUID;

public class TalentReserveNotFoundException extends ResourceNotFoundException {

    private TalentReserveNotFoundException(String message) {
        super(message);
    }

    public static TalentReserveNotFoundException reserve(UUID id) {
        return new TalentReserveNotFoundException(
                "Кадровый резерв с идентификатором " + id + " не найден"
        );
    }

    public static TalentReserveNotFoundException candidate(UUID id) {
        return new TalentReserveNotFoundException(
                "Кандидат кадрового резерва с идентификатором " + id + " не найден"
        );
    }

    public static TalentReserveNotFoundException decision(UUID id) {
        return new TalentReserveNotFoundException(
                "Кадровое решение с идентификатором " + id + " не найдено"
        );
    }
}
