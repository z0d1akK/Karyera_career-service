package com.zodiakk.careerservice.career.mapper;

import com.zodiakk.careerservice.career.dto.request.create.CareerTrackNodeCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.update.CareerTrackNodeUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.response.CareerTrackNodeResponseDto;
import com.zodiakk.careerservice.career.entity.CareerTrackNode;
import com.zodiakk.careerservice.config.MapStructConfig;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface CareerTrackNodeMapper {

    CareerTrackNode toEntity(CareerTrackNodeCreateRequestDto dto);

    CareerTrackNodeResponseDto toResponseDto(CareerTrackNode entity);

    void updateEntity(CareerTrackNodeUpdateRequestDto dto, @MappingTarget CareerTrackNode entity);
}
