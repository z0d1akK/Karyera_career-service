package com.zodiakk.careerservice.development.mapper;

import com.zodiakk.careerservice.config.MapStructConfig;
import com.zodiakk.careerservice.development.dto.request.create.PlanReviewCreateRequestDto;
import com.zodiakk.careerservice.development.dto.request.update.PlanReviewUpdateRequestDto;
import com.zodiakk.careerservice.development.dto.response.PlanReviewResponseDto;
import com.zodiakk.careerservice.development.entity.PlanReview;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(config = MapStructConfig.class)
public interface PlanReviewMapper {

    PlanReview toEntity(PlanReviewCreateRequestDto dto);

    PlanReviewResponseDto toResponseDto(PlanReview entity);

    void updateEntity(PlanReviewUpdateRequestDto dto, @MappingTarget PlanReview entity);
}
