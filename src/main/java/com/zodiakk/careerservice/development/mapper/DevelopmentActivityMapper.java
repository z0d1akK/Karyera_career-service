package com.zodiakk.careerservice.development.mapper;

import com.zodiakk.careerservice.config.MapStructConfig;
import com.zodiakk.careerservice.development.dto.request.create.DevelopmentActivityCreateRequestDto;
import com.zodiakk.careerservice.development.dto.request.update.DevelopmentActivityUpdateRequestDto;
import com.zodiakk.careerservice.development.dto.response.DevelopmentActivityResponseDto;
import com.zodiakk.careerservice.development.entity.DevelopmentActivity;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface DevelopmentActivityMapper {

    DevelopmentActivity toEntity(DevelopmentActivityCreateRequestDto dto);

    DevelopmentActivityResponseDto toResponseDto(DevelopmentActivity entity);

    void updateEntity(DevelopmentActivityUpdateRequestDto dto, @MappingTarget DevelopmentActivity entity);
}
