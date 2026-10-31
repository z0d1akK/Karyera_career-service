package com.zodiakk.careerservice.development.mapper;

import com.zodiakk.careerservice.config.MapStructConfig;
import com.zodiakk.careerservice.development.dto.request.create.DevelopmentPlanCreateRequestDto;
import com.zodiakk.careerservice.development.dto.request.update.DevelopmentPlanUpdateRequestDto;
import com.zodiakk.careerservice.development.dto.response.DevelopmentPlanResponseDto;
import com.zodiakk.careerservice.development.entity.DevelopmentPlan;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface DevelopmentPlanMapper {

    DevelopmentPlan toEntity(DevelopmentPlanCreateRequestDto dto);

    DevelopmentPlanResponseDto toResponseDto(DevelopmentPlan entity);

    void updateEntity(DevelopmentPlanUpdateRequestDto dto, @MappingTarget DevelopmentPlan entity);
}
