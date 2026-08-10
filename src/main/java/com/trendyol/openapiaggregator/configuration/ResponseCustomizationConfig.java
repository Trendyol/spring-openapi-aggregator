package com.trendyol.openapiaggregator.configuration;

import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "openapi.aggregator.response-customization")
@Data
public class ResponseCustomizationConfig {
    private Map<String, List<CustomResponse>> tagResponses = new HashMap<>();

    @Data
    public static class CustomResponse {
        private @Nullable String statusCode;
        private @Nullable String description;
        private @Nullable String schemaRef;
        private @Nullable SchemaDefinition inlineSchema;
    }

    @Data
    public static class SchemaDefinition {
        private @Nullable String type;
        private @Nullable String description;
        private @Nullable Map<String, PropertyDefinition> properties;
        private @Nullable List<String> required;
    }

    @Data
    public static class PropertyDefinition {
        private @Nullable String type;
        private @Nullable String format;
        private @Nullable String description;
        private @Nullable List<String> enumValues;
    }
}
