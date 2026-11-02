package com.zodiakk.careerservice;

import com.zodiakk.careerservice.common.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Career service application context")
class ApplicationContextIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Test
    @DisplayName("loads the application context with infrastructure beans")
    void shouldLoadContext() {
        assertThat(applicationContext).isNotNull();
        assertThat(kafkaTemplate).isNotNull();
        assertThat(mockMvc).isNotNull();
    }
}
