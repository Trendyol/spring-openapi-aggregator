package com.trendyol.openapiaggregator.mvc.autoconfigure;

import com.trendyol.openapiaggregator.EnableOpenApiAggregator;
import com.trendyol.openapiaggregator.configuration.OpenApiCacheConfig;
import com.trendyol.openapiaggregator.configuration.OpenApiFilterConfig;
import com.trendyol.openapiaggregator.configuration.OpenApiInfoConfig;
import com.trendyol.openapiaggregator.configuration.OpenApiOverridesConfig;
import com.trendyol.openapiaggregator.configuration.ResponseCustomizationConfig;
import com.trendyol.openapiaggregator.configuration.YamlPropertySourceFactory;
import com.trendyol.openapiaggregator.mvc.rest.MvcGatewayOpenApiAggregator;
import com.trendyol.openapiaggregator.mvc.service.RouteDefinitionAccessor;
import com.trendyol.openapiaggregator.service.OpenApiAggregatorCacheService;
import com.trendyol.openapiaggregator.service.OpenApiRouteCustomizer;
import com.trendyol.openapiaggregator.transformers.PrefixPathOpenApiFilterTransformer;
import com.trendyol.openapiaggregator.transformers.AddRequestParameterOpenApiFilterTransformer;
import com.trendyol.openapiaggregator.transformers.RemoveRequestParameterOpenApiFilterTransformer;
import com.trendyol.openapiaggregator.transformers.RewritePathOpenApiFilterTransformer;
import com.trendyol.openapiaggregator.transformers.SetPathOpenApiFilterTransformer;
import com.trendyol.openapiaggregator.transformers.StripPrefixOpenApiFilterTransformer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
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
@EnableConfigurationProperties({OpenApiFilterConfig.class, OpenApiInfoConfig.class, OpenApiOverridesConfig.class, ResponseCustomizationConfig.class, OpenApiCacheConfig.class})
@Import({MvcGatewayOpenApiAggregator.class, OpenApiRouteCustomizer.class, OpenApiAggregatorCacheService.class,
        PrefixPathOpenApiFilterTransformer.class, StripPrefixOpenApiFilterTransformer.class,
        AddRequestParameterOpenApiFilterTransformer.class, RemoveRequestParameterOpenApiFilterTransformer.class,
        RewritePathOpenApiFilterTransformer.class, SetPathOpenApiFilterTransformer.class})
public class OpenApiAggregatorAutoConfiguration {
    @Bean
    RouteDefinitionAccessor routeDefinitionAccessor(GatewayMvcProperties properties) {
        return new RouteDefinitionAccessor(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    RestClient.Builder openApiAggregatorRestClientBuilder() { return RestClient.builder(); }
}
