package com.zodiakk.careerservice.career.mapper;

import com.zodiakk.careerservice.career.dto.request.create.EmployeeCareerGoalCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.update.EmployeeCareerGoalUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.response.EmployeeCareerGoalResponseDto;
import com.zodiakk.careerservice.career.entity.EmployeeCareerGoal;
import com.zodiakk.careerservice.config.MapStructConfig;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface EmployeeCareerGoalMapper {

    EmployeeCareerGoal toEntity(EmployeeCareerGoalCreateRequestDto dto);

    EmployeeCareerGoalResponseDto toResponseDto(EmployeeCareerGoal entity);

    void updateEntity(EmployeeCareerGoalUpdateRequestDto dto, @MappingTarget EmployeeCareerGoal entity);
}
