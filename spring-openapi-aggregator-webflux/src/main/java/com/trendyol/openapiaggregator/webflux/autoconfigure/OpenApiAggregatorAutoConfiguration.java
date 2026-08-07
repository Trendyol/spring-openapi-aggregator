package com.trendyol.openapiaggregator.webflux.autoconfigure;

import com.trendyol.openapiaggregator.EnableOpenApiAggregator;
import com.trendyol.openapiaggregator.configuration.OpenApiFilterConfig;
import com.trendyol.openapiaggregator.configuration.OpenApiInfoConfig;
import com.trendyol.openapiaggregator.configuration.OpenApiOverridesConfig;
import com.trendyol.openapiaggregator.configuration.ResponseCustomizationConfig;
import com.trendyol.openapiaggregator.configuration.YamlPropertySourceFactory;
import com.trendyol.openapiaggregator.service.OpenApiRouteCustomizer;
import com.trendyol.openapiaggregator.webflux.rest.GatewayOpenApiAggregator;
import com.trendyol.openapiaggregator.webflux.service.RouteDefinitionAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.reactive.function.client.WebClient;

@AutoConfiguration
@ConditionalOnBean(annotation = EnableOpenApiAggregator.class)
@ConditionalOnClass({RouteDefinitionLocator.class, WebClient.class})
@PropertySource(value = "classpath:openapi-aggregator-defaults.yml", factory = YamlPropertySourceFactory.class)
@EnableConfigurationProperties({OpenApiFilterConfig.class, OpenApiInfoConfig.class, OpenApiOverridesConfig.class, ResponseCustomizationConfig.class})
@Import({GatewayOpenApiAggregator.class, OpenApiRouteCustomizer.class, RouteDefinitionAccessor.class})
public class OpenApiAggregatorAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    ObjectMapper openApiAggregatorObjectMapper() { return new ObjectMapper(); }

    @Bean
    @ConditionalOnMissingBean(WebClient.class)
    WebClient.Builder openApiAggregatorWebClientBuilder() { return WebClient.builder(); }
}
