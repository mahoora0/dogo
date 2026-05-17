package com.example.dogo.config;

import com.example.dogo.service.match.semantic.SemanticMatchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(SemanticMatchProperties.class)
public class SemanticMatchConfig {
}
