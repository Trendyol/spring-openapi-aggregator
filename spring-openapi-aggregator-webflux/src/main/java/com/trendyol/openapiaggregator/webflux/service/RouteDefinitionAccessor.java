package com.trendyol.openapiaggregator.webflux.service;

import com.trendyol.openapiaggregator.dto.RouteInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class RouteDefinitionAccessor {
    private final RouteDefinitionLocator routeDefinitionLocator;

    public Mono<RouteDefinition> getRouteDefinition(String routeId) {
        return routeDefinitionLocator.getRouteDefinitions()
                .filter(definition -> Objects.equals(definition.getId(), routeId))
                .next();
    }

    public Mono<Map<String, Object>> getFilterConfigs(String routeId) {
        return getRouteDefinition(routeId).map(definition -> {
            Map<String, Object> filterConfigs = new LinkedHashMap<>();
            definition.getFilters().forEach(filter -> filterConfigs.put(
                    Objects.requireNonNull(filter.getName(), "Filter name must not be null"),
                    filter.getArgs()));
            return filterConfigs;
        }).defaultIfEmpty(Collections.emptyMap());
    }

    public Mono<RouteInfo> getCompleteRouteInfo(String routeId) {
        return getRouteDefinition(routeId).map(definition -> {
            Map<String, Map<String, String>> predicates = toPredicateMap(definition.getPredicates());
            Map<String, Map<String, String>> filters = toFilterMap(definition.getFilters());
            return new RouteInfo(
                    Objects.requireNonNull(definition.getId(), "Route definition id must not be null"),
                    Objects.requireNonNull(definition.getUri(), "Route definition URI must not be null").toString(),
                    predicates,
                    filters);
        });
    }

    public Mono<Map<String, Map<String, String>>> getPredicates(String routeId) {
        return getRouteDefinition(routeId).map(definition -> toPredicateMap(definition.getPredicates()))
                .defaultIfEmpty(Collections.emptyMap());
    }

    private Map<String, Map<String, String>> toPredicateMap(List<PredicateDefinition> definitions) {
        Map<String, Map<String, String>> predicates = new LinkedHashMap<>();
        definitions.forEach(predicate -> predicates.put(
                Objects.requireNonNull(predicate.getName(), "Predicate name must not be null"),
                normalizeArgs(predicate.getArgs())));
        return predicates;
    }

    private Map<String, Map<String, String>> toFilterMap(List<FilterDefinition> definitions) {
        Map<String, Map<String, String>> filters = new LinkedHashMap<>();
        definitions.forEach(filter -> filters.put(
                Objects.requireNonNull(filter.getName(), "Filter name must not be null"),
                normalizeArgs(filter.getArgs())));
        return filters;
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
