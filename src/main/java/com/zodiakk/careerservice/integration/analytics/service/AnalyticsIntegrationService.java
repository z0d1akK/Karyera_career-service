package com.zodiakk.careerservice.integration.analytics.service;

import com.zodiakk.careerservice.common.exception.IntegrationUnavailableException;
import com.zodiakk.careerservice.integration.analytics.client.AnalyticsScreeningClient;
import com.zodiakk.careerservice.integration.analytics.dto.ScreeningStartRequestDto;
import com.zodiakk.careerservice.integration.analytics.dto.ScreeningStartResponseDto;
import com.zodiakk.careerservice.integration.analytics.dto.TalentScreeningDto;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalyticsIntegrationService {

    private static final String ANALYTICS_UNAVAILABLE =
            "Сервис аналитики недоступен. Предварительный отбор временно невозможен";

    private final AnalyticsScreeningClient client;

    public ScreeningStartResponseDto startScreening(ScreeningStartRequestDto request) {
        try {
            return client.startScreening(request);
        } catch (FeignException ex) {
            throw unavailable(ex);
        }
    }

    public TalentScreeningDto getScreening(UUID jobId) {
        try {
            return client.getScreening(jobId);
        } catch (FeignException ex) {
            throw unavailable(ex);
        }
    }

    private IntegrationUnavailableException unavailable(FeignException cause) {
        log.warn("Analytics service call failed: {}", cause.getMessage());
        return new IntegrationUnavailableException(ANALYTICS_UNAVAILABLE, cause);
    }
}
