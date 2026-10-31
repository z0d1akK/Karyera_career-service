package com.zodiakk.careerservice.talent.mapper;

import com.zodiakk.careerservice.config.MapStructConfig;
import com.zodiakk.careerservice.talent.dto.request.create.TalentReserveCreateRequestDto;
import com.zodiakk.careerservice.talent.dto.request.update.TalentReserveUpdateRequestDto;
import com.zodiakk.careerservice.talent.dto.response.TalentReserveResponseDto;
import com.zodiakk.careerservice.talent.entity.TalentReserve;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface TalentReserveMapper {

    TalentReserve toEntity(TalentReserveCreateRequestDto dto);

    TalentReserveResponseDto toResponseDto(TalentReserve entity);

    void updateEntity(TalentReserveUpdateRequestDto dto, @MappingTarget TalentReserve entity);
}
