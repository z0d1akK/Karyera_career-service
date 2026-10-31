package com.zodiakk.careerservice.career.mapper;

import com.zodiakk.careerservice.career.dto.request.create.CareerTransitionCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.update.CareerTransitionUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.response.CareerTransitionResponseDto;
import com.zodiakk.careerservice.career.entity.CareerTransition;
import com.zodiakk.careerservice.config.MapStructConfig;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface CareerTransitionMapper {

    CareerTransition toEntity(CareerTransitionCreateRequestDto dto);

    CareerTransitionResponseDto toResponseDto(CareerTransition entity);

    void updateEntity(CareerTransitionUpdateRequestDto dto, @MappingTarget CareerTransition entity);
}
