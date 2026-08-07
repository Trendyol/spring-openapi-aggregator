package com.trendyol.openapiaggregator.mvc.autoconfigure;

import com.trendyol.openapiaggregator.EnableOpenApiAggregator;
import com.trendyol.openapiaggregator.configuration.OpenApiFilterConfig;
import com.trendyol.openapiaggregator.configuration.OpenApiInfoConfig;
import com.trendyol.openapiaggregator.configuration.OpenApiOverridesConfig;
import com.trendyol.openapiaggregator.configuration.ResponseCustomizationConfig;
import com.trendyol.openapiaggregator.configuration.YamlPropertySourceFactory;
import com.trendyol.openapiaggregator.mvc.rest.GatewayOpenApiAggregator;
import com.trendyol.openapiaggregator.mvc.service.RouteDefinitionAccessor;
import com.trendyol.openapiaggregator.service.OpenApiRouteCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.client.RestClient;

@AutoConfiguration
@ConditionalOnBean(annotation = EnableOpenApiAggregator.class)
@ConditionalOnMissingClass("org.springframework.cloud.gateway.route.RouteDefinitionLocator")
@ConditionalOnClass({GatewayMvcProperties.class, RestClient.class})
@PropertySource(value = "classpath:openapi-aggregator-defaults.yml", factory = YamlPropertySourceFactory.class)
@EnableConfigurationProperties({OpenApiFilterConfig.class, OpenApiInfoConfig.class, OpenApiOverridesConfig.class, ResponseCustomizationConfig.class})
@Import({GatewayOpenApiAggregator.class, OpenApiRouteCustomizer.class})
public class OpenApiAggregatorAutoConfiguration {
    @Bean
    RouteDefinitionAccessor routeDefinitionAccessor(GatewayMvcProperties properties) {
        return new RouteDefinitionAccessor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    RestClient.Builder openApiAggregatorRestClientBuilder() { return RestClient.builder(); }
}
