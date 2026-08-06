package com.trendyol.openapiaggregator.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "openapi.aggregator.info")
public class OpenApiInfoConfig {
    private String title = "Unified Gateway API";
    private String description = "Unified API documentation";
    private String version = "1.0.0";
    private String noServicesDescription = "No services available";
}
