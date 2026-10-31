package com.zodiakk.careerservice.development.mapper;

import com.zodiakk.careerservice.config.MapStructConfig;
import com.zodiakk.careerservice.development.dto.request.create.ActivityEvidenceCreateRequestDto;
import com.zodiakk.careerservice.development.dto.request.update.ActivityEvidenceUpdateRequestDto;
import com.zodiakk.careerservice.development.dto.response.ActivityEvidenceResponseDto;
import com.zodiakk.careerservice.development.entity.ActivityEvidence;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface ActivityEvidenceMapper {

    ActivityEvidence toEntity(ActivityEvidenceCreateRequestDto dto);

    ActivityEvidenceResponseDto toResponseDto(ActivityEvidence entity);

    void updateEntity(ActivityEvidenceUpdateRequestDto dto, @MappingTarget ActivityEvidence entity);
}
