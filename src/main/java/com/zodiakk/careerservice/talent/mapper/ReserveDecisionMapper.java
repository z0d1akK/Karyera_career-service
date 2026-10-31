package com.zodiakk.careerservice.talent.mapper;

import com.zodiakk.careerservice.config.MapStructConfig;
import com.zodiakk.careerservice.talent.dto.request.create.ReserveDecisionCreateRequestDto;
import com.zodiakk.careerservice.talent.dto.request.update.ReserveDecisionUpdateRequestDto;
import com.zodiakk.careerservice.talent.dto.response.ReserveDecisionResponseDto;
import com.zodiakk.careerservice.talent.entity.ReserveDecision;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface ReserveDecisionMapper {

    ReserveDecision toEntity(ReserveDecisionCreateRequestDto dto);

    ReserveDecisionResponseDto toResponseDto(ReserveDecision entity);

    void updateEntity(ReserveDecisionUpdateRequestDto dto, @MappingTarget ReserveDecision entity);
}
