package com.trendyol.openapiaggregator.configuration;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "openapi.aggregator")
@Data
public class OpenApiOverridesConfig {
    private Map<String, ServiceOverrides> overrides = new HashMap<>();

    @Data
    @NoArgsConstructor
    public static class ServiceOverrides {
        private String baseUrl = "/v3/api-docs";
    }
}
