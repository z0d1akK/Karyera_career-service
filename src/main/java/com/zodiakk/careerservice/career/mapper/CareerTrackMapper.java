package com.zodiakk.careerservice.career.mapper;

import com.zodiakk.careerservice.career.dto.request.create.CareerTrackCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.update.CareerTrackUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.response.CareerTrackResponseDto;
import com.zodiakk.careerservice.career.entity.CareerTrack;
import com.zodiakk.careerservice.config.MapStructConfig;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface CareerTrackMapper {

    CareerTrack toEntity(CareerTrackCreateRequestDto dto);

    CareerTrackResponseDto toResponseDto(CareerTrack entity);

    void updateEntity(CareerTrackUpdateRequestDto dto, @MappingTarget CareerTrack entity);
}
