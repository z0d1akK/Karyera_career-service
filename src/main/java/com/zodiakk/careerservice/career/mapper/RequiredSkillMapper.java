package com.zodiakk.careerservice.career.mapper;

import com.zodiakk.careerservice.career.dto.request.create.RequiredSkillCreateRequestDto;
import com.zodiakk.careerservice.career.dto.request.update.RequiredSkillUpdateRequestDto;
import com.zodiakk.careerservice.career.dto.response.RequiredSkillResponseDto;
import com.zodiakk.careerservice.career.entity.RequiredSkill;
import com.zodiakk.careerservice.config.MapStructConfig;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface RequiredSkillMapper {

    RequiredSkill toEntity(RequiredSkillCreateRequestDto dto);

    RequiredSkillResponseDto toResponseDto(RequiredSkill entity);

    void updateEntity(RequiredSkillUpdateRequestDto dto, @MappingTarget RequiredSkill entity);
}
