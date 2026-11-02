package com.zodiakk.careerservice.common.exception.domain;

import com.zodiakk.careerservice.common.exception.ConflictException;

public class ImmutableResourceException extends ConflictException {

    public ImmutableResourceException(String message) {
        super(message);
    }

    public static ImmutableResourceException publishedRequirement() {
        return new ImmutableResourceException(
                "Опубликованная версия требований не может быть изменена"
        );
    }

    public static ImmutableResourceException publishedTrackStructure() {
        return new ImmutableResourceException(
                "Структура опубликованного карьерного трека не может быть изменена напрямую"
        );
    }
}
