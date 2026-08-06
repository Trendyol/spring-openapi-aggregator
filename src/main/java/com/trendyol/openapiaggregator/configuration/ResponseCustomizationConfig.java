package com.trendyol.openapiaggregator.configuration;

import lombok.Data;
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
        private String statusCode;
        private String description;
        private String schemaRef;
        private SchemaDefinition inlineSchema;
    }

    @Data
    public static class SchemaDefinition {
        private String type;
        private String description;
        private Map<String, PropertyDefinition> properties;
        private List<String> required;
    }

    @Data
    public static class PropertyDefinition {
        private String type;
        private String format;
        private String description;
        private List<String> enumValues;
    }
}
