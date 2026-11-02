package com.zodiakk.careerservice.config;

import com.zodiakk.careerservice.common.constants.KafkaEventConstants;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topics.career-events}")
    private String careerEventsTopic;

    @Value("${app.kafka.topics.employee-events}")
    private String employeeEventsTopic;

    @Value("${app.kafka.topics.analytics-events}")
    private String analyticsEventsTopic;

    @Bean
    public NewTopic careerEventsTopic() {
        return buildTopic(careerEventsTopic);
    }

    @Bean
    public NewTopic careerEventsDltTopic() {
        return buildTopic(careerEventsTopic + KafkaEventConstants.DLT_SUFFIX);
    }

    @Bean
    public NewTopic employeeEventsTopic() {
        return buildTopic(employeeEventsTopic);
    }

    @Bean
    public NewTopic employeeEventsDltTopic() {
        return buildTopic(employeeEventsTopic + KafkaEventConstants.DLT_SUFFIX);
    }

    @Bean
    public NewTopic analyticsEventsTopic() {
        return buildTopic(analyticsEventsTopic);
    }

    @Bean
    public NewTopic analyticsEventsDltTopic() {
        return buildTopic(analyticsEventsTopic + KafkaEventConstants.DLT_SUFFIX);
    }

    private NewTopic buildTopic(String name) {
        return TopicBuilder.name(name)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
