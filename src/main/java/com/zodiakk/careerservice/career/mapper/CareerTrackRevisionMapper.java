package com.zodiakk.careerservice.career.mapper;

import com.zodiakk.careerservice.career.dto.request.create.CareerTrackRevisionCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.update.CareerTrackRevisionUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.response.CareerTrackRevisionResponseDto;
import com.zodiakk.careerservice.career.entity.CareerTrackRevision;
import com.zodiakk.careerservice.config.MapStructConfig;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface CareerTrackRevisionMapper {

    CareerTrackRevision toEntity(CareerTrackRevisionCreateRequestDto dto);

    CareerTrackRevisionResponseDto toResponseDto(CareerTrackRevision entity);

    void updateEntity(CareerTrackRevisionUpdateRequestDto dto, @MappingTarget CareerTrackRevision entity);
}
