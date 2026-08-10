package com.trendyol.openapiaggregator.webflux.rest;

import com.trendyol.openapiaggregator.configuration.OpenApiOverridesConfig;
import com.trendyol.openapiaggregator.exception.OpenApiAggregatorException;
import com.trendyol.openapiaggregator.service.OpenApiRouteCustomizer;
import com.trendyol.openapiaggregator.webflux.service.RouteDefinitionAccessor;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.CUSTOMIZED;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.DOCS_PATH;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.EMPTY;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.MISSING;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.OPENAPI_VERSION;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.ORDERS;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.ORDERS_URI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReactiveGatewayOpenApiAggregatorTest {
    private static RouteDefinition route(String id, String uri) {
        RouteDefinition route = new RouteDefinition();
        route.setId(id);
        route.setUri(URI.create(uri));
        return route;
    }

    @Test
    void fetchesAndCustomizesAServiceDocumentUsingItsOverride() {
        RouteDefinition route = route(ORDERS, ORDERS_URI);
        RouteDefinitionLocator locator = () -> Flux.just(route);
        RouteDefinitionAccessor routeAccessor = mock(RouteDefinitionAccessor.class);
        when(routeAccessor.getPredicates(ORDERS)).thenReturn(Mono.just(Map.of()));
        when(routeAccessor.getFilterConfigs(ORDERS)).thenReturn(Mono.just(Map.of()));
        WebClient.Builder builder = WebClient.builder().exchangeFunction(ignored ->
                Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header("Content-Type", "application/json")
                        .body("{\"openapi\":\"" + OPENAPI_VERSION + "\",\"paths\":{}}")
                        .build()));
        OpenApiRouteCustomizer customizer = mock(OpenApiRouteCustomizer.class);
        when(customizer.applyOpenApiCustomisations(any(), any(), any())).thenReturn(CUSTOMIZED);
        OpenApiOverridesConfig overrides = new OpenApiOverridesConfig();
        OpenApiOverridesConfig.ServiceOverrides service = new OpenApiOverridesConfig.ServiceOverrides();
        service.setBaseUrl(DOCS_PATH);
        overrides.getOverrides().put(ORDERS, service);

        ReactiveGatewayOpenApiAggregator aggregator = new ReactiveGatewayOpenApiAggregator(
                locator, routeAccessor, builder, customizer, overrides);

        assertThat(aggregator.getServiceDocs(ORDERS).block()).isEqualTo(CUSTOMIZED);
    }

    @Test
    void returnsEmptyUnifiedSpecWhenAServiceDocumentCannotBeFetched() {
        RouteDefinitionLocator locator = () -> Flux.just(route(ORDERS, ORDERS_URI));
        RouteDefinitionAccessor routeAccessor = mock(RouteDefinitionAccessor.class);
        when(routeAccessor.getPredicates(ORDERS)).thenReturn(Mono.just(Map.of()));
        when(routeAccessor.getFilterConfigs(ORDERS)).thenReturn(Mono.just(Map.of()));
        WebClient.Builder builder = WebClient.builder().exchangeFunction(ignored -> Mono.error(new RuntimeException("down")));
        OpenApiRouteCustomizer customizer = mock(OpenApiRouteCustomizer.class);
        when(customizer.mergeApiDocs(List.of())).thenReturn(EMPTY);

        ReactiveGatewayOpenApiAggregator aggregator = new ReactiveGatewayOpenApiAggregator(
                locator, routeAccessor, builder, customizer, new OpenApiOverridesConfig());

        assertThat(aggregator.getUnifiedDocs().block()).isEqualTo(EMPTY);
    }

    @Test
    void reportsUnknownServiceInsteadOfCallingTheClient() {
        ReactiveGatewayOpenApiAggregator aggregator = new ReactiveGatewayOpenApiAggregator(
                Flux::empty, mock(RouteDefinitionAccessor.class), WebClient.builder(),
                mock(OpenApiRouteCustomizer.class), new OpenApiOverridesConfig());

        assertThatThrownBy(() -> aggregator.getServiceDocs(MISSING).block())
                .isInstanceOf(OpenApiAggregatorException.class)
                .hasMessage("Service route not found: " + MISSING);
    }
}
