package com.trendyol.openapiaggregator.mvc.service;

import com.trendyol.openapiaggregator.service.GatewayRouteAccessor;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class RouteDefinitionAccessor implements GatewayRouteAccessor {
    private final GatewayMvcProperties properties;

    public RouteDefinitionAccessor(GatewayMvcProperties properties) {
        this.properties = properties;
    }

    @Override
    public Map<String, Map<String, String>> getPredicates(String routeId) {
        return find(routeId).map(route -> route.getPredicates().stream().collect(java.util.stream.Collectors.toMap(
                predicate -> predicate.getName(), predicate -> predicate.getArgs(),
                (first, second) -> first, LinkedHashMap::new)))
                .map(value -> (Map<String, Map<String, String>>) (Map<?, ?>) value)
                .orElseGet(Collections::emptyMap);
    }

    @Override
    public Map<String, Object> getFilterConfigs(String routeId) {
        return find(routeId).map(route -> route.getFilters().stream().collect(java.util.stream.Collectors.toMap(
                filter -> filter.getName(), filter -> filter.getArgs(),
                (first, second) -> first, LinkedHashMap::new)))
                .map(value -> (Map<String, Object>) (Map<?, ?>) value)
                .orElseGet(Collections::emptyMap);
    }

    public java.util.List<RouteProperties> getRoutes() {
        return properties.getRoutes();
    }

    private java.util.Optional<RouteProperties> find(String routeId) {
        return properties.getRoutes().stream().filter(route -> routeId.equals(route.getId())).findFirst();
    }
}
