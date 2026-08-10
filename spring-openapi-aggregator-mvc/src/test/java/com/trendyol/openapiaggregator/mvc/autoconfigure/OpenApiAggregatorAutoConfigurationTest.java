package com.trendyol.openapiaggregator.mvc.autoconfigure;

import com.trendyol.openapiaggregator.EnableOpenApiAggregator;
import com.trendyol.openapiaggregator.mvc.rest.MvcGatewayOpenApiAggregator;
import com.trendyol.openapiaggregator.mvc.service.RouteDefinitionAccessor;
import com.trendyol.openapiaggregator.service.OpenApiRouteCustomizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiAggregatorAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(OpenApiAggregatorAutoConfiguration.class))
            .withBean(GatewayMvcProperties.class, GatewayMvcProperties::new)
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withBean(RestClient.Builder.class, RestClient::builder);

    @Test
    void doesNotCreateAggregatorBeansWithoutEnableAnnotation() {
        contextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(MvcGatewayOpenApiAggregator.class)
                .doesNotHaveBean(OpenApiRouteCustomizer.class)
                .doesNotHaveBean(RouteDefinitionAccessor.class));
    }

    @Test
    void createsAggregatorBeansWhenEnableAnnotationIsPresent() {
        contextRunner.withClassLoader(new FilteredClassLoader(RouteDefinitionLocator.class))
                .withUserConfiguration(EnabledConfiguration.class)
                .run(context -> assertThat(context)
                        .hasSingleBean(MvcGatewayOpenApiAggregator.class)
                        .hasSingleBean(OpenApiRouteCustomizer.class)
                        .hasSingleBean(RouteDefinitionAccessor.class));
    }

    @Test
    void backsOffWhenWebFluxGatewayIsAvailable() {
        contextRunner.withUserConfiguration(EnabledConfiguration.class)
                .run(context -> assertThat(context)
                        .doesNotHaveBean(MvcGatewayOpenApiAggregator.class)
                        .doesNotHaveBean(OpenApiRouteCustomizer.class)
                        .doesNotHaveBean(RouteDefinitionAccessor.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableOpenApiAggregator
    static class EnabledConfiguration {
    }
}
