package com.trendyol.openapiaggregator.configuration;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "openapi.aggregator")
@Data
public class OpenApiFilterConfig {
    private Map<String, ServiceFilter> filters = new HashMap<>();
    private List<String> disabledTransformers = List.of();

    @Data
    @NoArgsConstructor
    public static class ServiceFilter {
        private List<String> includeTags = List.of();
        private List<String> excludeTags = List.of();
        private List<String> excludePaths = List.of();
    }
}
