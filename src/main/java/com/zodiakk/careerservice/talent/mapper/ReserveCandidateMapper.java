package com.zodiakk.careerservice.talent.mapper;

import com.zodiakk.careerservice.config.MapStructConfig;
import com.zodiakk.careerservice.talent.dto.request.create.ReserveCandidateCreateRequestDto;
import com.zodiakk.careerservice.talent.dto.request.update.ReserveCandidateUpdateRequestDto;
import com.zodiakk.careerservice.talent.dto.response.ReserveCandidateResponseDto;
import com.zodiakk.careerservice.talent.entity.ReserveCandidate;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface ReserveCandidateMapper {

    ReserveCandidate toEntity(ReserveCandidateCreateRequestDto dto);

    ReserveCandidateResponseDto toResponseDto(ReserveCandidate entity);

    void updateEntity(ReserveCandidateUpdateRequestDto dto, @MappingTarget ReserveCandidate entity);
}
