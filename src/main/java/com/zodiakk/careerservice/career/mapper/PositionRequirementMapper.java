package com.zodiakk.careerservice.career.mapper;

import com.zodiakk.careerservice.career.dto.request.create.PositionRequirementCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.update.PositionRequirementUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.response.PositionRequirementResponseDto;
import com.zodiakk.careerservice.career.entity.PositionRequirement;
import com.zodiakk.careerservice.config.MapStructConfig;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface PositionRequirementMapper {

    PositionRequirement toEntity(PositionRequirementCreateRequestDto dto);

    PositionRequirementResponseDto toResponseDto(PositionRequirement entity);

    void updateEntity(PositionRequirementUpdateRequestDto dto, @MappingTarget PositionRequirement entity);
}
