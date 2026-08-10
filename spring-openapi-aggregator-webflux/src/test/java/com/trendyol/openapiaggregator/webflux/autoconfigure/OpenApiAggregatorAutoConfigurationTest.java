package com.trendyol.openapiaggregator.webflux.autoconfigure;

import com.trendyol.openapiaggregator.EnableOpenApiAggregator;
import com.trendyol.openapiaggregator.service.OpenApiRouteCustomizer;
import com.trendyol.openapiaggregator.webflux.rest.ReactiveGatewayOpenApiAggregator;
import com.trendyol.openapiaggregator.webflux.service.RouteDefinitionAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiAggregatorAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenApiAggregatorAutoConfiguration.class))
            .withUserConfiguration(RouteConfiguration.class);

    @Test
    void doesNotCreateAggregatorBeansWithoutEnableAnnotation() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(ReactiveGatewayOpenApiAggregator.class)
                .doesNotHaveBean(OpenApiRouteCustomizer.class)
                .doesNotHaveBean(RouteDefinitionAccessor.class));
    }

    @Test
    void createsAggregatorBeansWhenEnableAnnotationIsPresent() {
        contextRunner.withUserConfiguration(EnabledConfiguration.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(ReactiveGatewayOpenApiAggregator.class)
                        .hasSingleBean(OpenApiRouteCustomizer.class)
                        .hasSingleBean(RouteDefinitionAccessor.class));
    }

    @Configuration(proxyBeanMethods = false)
    static class RouteConfiguration {
        @Bean
        RouteDefinitionLocator routeDefinitionLocator() {
            return Flux::empty;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableOpenApiAggregator
    static class EnabledConfiguration {
    }
}
