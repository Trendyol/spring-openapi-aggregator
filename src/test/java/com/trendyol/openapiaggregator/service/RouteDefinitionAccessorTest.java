package com.trendyol.openapiaggregator.service;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import com.trendyol.openapiaggregator.dto.RouteInfo;
import com.trendyol.openapiaggregator.webflux.service.RouteDefinitionAccessor;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.ORDERS;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.ORDERS_URI;
import static com.trendyol.openapiaggregator.service.OpenApiTestConstants.MISSING;

class RouteDefinitionAccessorTest {
    @Test
    void returnsRouteDetailsPredicatesAndFilters() {
        RouteDefinition route = new RouteDefinition();
        route.setId(ORDERS);
        route.setUri(URI.create(ORDERS_URI));
        route.setPredicates(List.of(new PredicateDefinition("Path=/orders/**"), new PredicateDefinition("Method=GET")));
        route.setFilters(List.of(new FilterDefinition("RewritePath=/orders/(?<path>.*), /${path}")));
        RouteDefinitionLocator locator = () -> Flux.just(route);

        RouteInfo result = new RouteDefinitionAccessor(locator).getCompleteRouteInfo(ORDERS).block();

        assertThat(result.getId()).isEqualTo(ORDERS);
        assertThat(result.getUri()).isEqualTo(ORDERS_URI);
        assertThat(result.getPredicates()).containsKeys("Path", "Method");
        assertThat(result.getFilters()).containsKey("RewritePath");
    }

    @Test
    void extractsFilterConfigsAndReturnsEmptyValuesForUnknownRoute() {
        RouteDefinition route = new RouteDefinition();
        route.setId(ORDERS);
        route.setFilters(List.of(new FilterDefinition("AddRequestHeader=X-Test, value")));
        RouteDefinitionLocator locator = () -> Flux.just(route);
        RouteDefinitionAccessor accessor = new RouteDefinitionAccessor(locator);

        assertThat(accessor.getFilterConfigs(ORDERS).block()).containsKey("AddRequestHeader");
        assertThat(accessor.getPredicates(MISSING).block()).isEmpty();
        assertThat(accessor.getCompleteRouteInfo(MISSING).block()).isNull();
    }
}
