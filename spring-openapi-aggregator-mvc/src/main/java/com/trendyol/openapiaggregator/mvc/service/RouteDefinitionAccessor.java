package com.trendyol.openapiaggregator.mvc.service;

import com.trendyol.openapiaggregator.service.GatewayRouteAccessor;
import org.springframework.cloud.gateway.server.mvc.config.GatewayMvcProperties;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class RouteDefinitionAccessor implements GatewayRouteAccessor {
    private final GatewayMvcProperties properties;

    public RouteDefinitionAccessor(GatewayMvcProperties properties) {
        this.properties = properties;
    }

    @Override
    public Map<String, Map<String, String>> getPredicates(String routeId) {
        return find(routeId).map(route -> {
            Map<String, Map<String, String>> predicates = new LinkedHashMap<>();
            route.getPredicates().forEach(predicate -> predicates.put(
                    Objects.requireNonNull(predicate.getName(), "Predicate name must not be null"),
                    normalizeArgs(predicate.getArgs())));
            return predicates;
        }).orElseGet(Collections::emptyMap);
    }

    @Override
    public Map<String, Object> getFilterConfigs(String routeId) {
        return find(routeId).map(route -> {
            Map<String, Object> filters = new LinkedHashMap<>();
            route.getFilters().forEach(filter -> filters.put(
                    Objects.requireNonNull(filter.getName(), "Filter name must not be null"),
                    filter.getArgs()));
            return filters;
        }).orElseGet(Collections::emptyMap);
    }

    public List<RouteProperties> getRoutes() {
        return properties.getRoutes();
    }

    private Optional<RouteProperties> find(String routeId) {
        return properties.getRoutes().stream().filter(route -> routeId.equals(route.getId())).findFirst();
    }

    private Map<String, String> normalizeArgs(Map<?, ?> arguments) {
        Map<String, String> normalized = new LinkedHashMap<>();
        arguments.forEach((key, value) -> {
            if (key instanceof String stringKey && value instanceof String stringValue) {
                normalized.put(stringKey, stringValue);
            }
        });
        return normalized;
    }
}
