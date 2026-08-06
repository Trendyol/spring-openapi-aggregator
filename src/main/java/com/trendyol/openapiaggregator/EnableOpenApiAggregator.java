package com.trendyol.openapiaggregator;

import com.trendyol.openapiaggregator.autoconfigure.OpenApiAggregatorAutoConfiguration;
import org.springframework.context.annotation.Import;

/**
 * Enabling annotation to activate the OpenAPI Aggregator library.
 * <p>
 * Add {@code @EnableOpenApiAggregator} to your Spring Boot application class
 * to import the aggregator controller, services and configuration properties.
 */
@Import(OpenApiAggregatorAutoConfiguration.class)
public @interface EnableOpenApiAggregator {
}
