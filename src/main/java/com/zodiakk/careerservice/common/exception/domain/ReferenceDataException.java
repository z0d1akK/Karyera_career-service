package com.zodiakk.careerservice.common.exception.domain;

import com.zodiakk.careerservice.common.exception.ConflictException;

public class ReferenceDataException extends ConflictException {

    public ReferenceDataException(String message) {
        super(message);
    }

    public static ReferenceDataException positionNotFound(Object id) {
        return new ReferenceDataException("Должность с идентификатором " + id + " не найдена");
    }

    public static ReferenceDataException gradeNotFound(Object id) {
        return new ReferenceDataException("Грейд с идентификатором " + id + " не найден");
    }

    public static ReferenceDataException skillNotFound(Object id) {
        return new ReferenceDataException("Навык с идентификатором " + id + " не найден");
    }

    public static ReferenceDataException employeeNotFound(Object id) {
        return new ReferenceDataException("Сотрудник с идентификатором " + id + " не найден");
    }
}
