package com.zodiakk.careerservice.common.exception.domain;

import com.zodiakk.careerservice.common.exception.ConflictException;

import java.util.UUID;

public class ActiveCareerGoalExistsException extends ConflictException {

    public ActiveCareerGoalExistsException(UUID employeeId) {
        super("У сотрудника " + employeeId + " уже есть активная карьерная цель");
    }
}
