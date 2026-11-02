package com.zodiakk.careerservice.integration.analytics.client;

import com.zodiakk.careerservice.integration.analytics.dto.ScreeningStartRequestDto;
import com.zodiakk.careerservice.integration.analytics.dto.ScreeningStartResponseDto;
import com.zodiakk.careerservice.integration.analytics.dto.TalentScreeningDto;
import com.zodiakk.careerservice.integration.config.IntegrationFeignConfig;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.UUID;

@FeignClient(
        name = "analytic-service",
        url = "${services.analytics.url}",
        configuration = IntegrationFeignConfig.class
)
public interface AnalyticsScreeningClient {

    @PostMapping("/internal/v1/analytics/talent-reserve/screenings")
    ScreeningStartResponseDto startScreening(@RequestBody ScreeningStartRequestDto request);

    @GetMapping("/internal/v1/analytics/talent-reserve/screenings/{jobId}")
    TalentScreeningDto getScreening(@PathVariable UUID jobId);
}
