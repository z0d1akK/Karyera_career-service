package com.zodiakk.careerservice.integration.config;

import com.zodiakk.careerservice.common.constants.Headers;
import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

/**
 * Isolated Feign configuration. It is intentionally not annotated with {@code @Configuration}
 * so the internal API key is only applied to the clients that reference it explicitly.
 */
public class IntegrationFeignConfig {

    @Bean
    public RequestInterceptor internalApiKeyInterceptor(
            @Value("${internal.api-key}") String internalApiKey
    ) {
        return template -> template.header(Headers.INTERNAL_KEY, internalApiKey);
    }
}
