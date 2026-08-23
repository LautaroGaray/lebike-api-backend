package com.example.scaffold.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:repository-queries.properties")
public class QueriesConfig {
}

