package com.trendyol.openapiaggregator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enabling annotation to activate the OpenAPI Aggregator library.
 * <p>
 * Add {@code @EnableOpenApiAggregator} to your Spring Boot application class
 * to import the aggregator controller, services and configuration properties.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EnableOpenApiAggregator {
}
