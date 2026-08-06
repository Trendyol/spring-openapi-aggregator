package com.trendyol.openapiaggregator.service;

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
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

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
        return getRouteDefinition(routeId)
                .map(definition -> {
                    Map<String, Object> filterConfigs = new LinkedHashMap<>();

                    definition.getFilters().forEach(filterDefinition -> {
                        String filterName = filterDefinition.getName();
                        Map<String, String> args = filterDefinition.getArgs();
                        filterConfigs.put(filterName, args);
                    });

                    return filterConfigs;
                })
                .defaultIfEmpty(Collections.emptyMap());
    }

    public Mono<RouteInfo> getCompleteRouteInfo(String routeId) {
        return getRouteDefinition(routeId)
                .map(definition -> {
                    RouteInfo info = new RouteInfo();
                    info.setId(definition.getId());
                    info.setUri(definition.getUri().toString());
                    info.setPredicates(definition.getPredicates().stream()
                            .collect(Collectors.toMap(
                                    PredicateDefinition::getName,
                                    PredicateDefinition::getArgs
                            ))
                    );
                    info.setFilters(definition.getFilters().stream()
                            .collect(Collectors.toMap(
                                    FilterDefinition::getName,
                                    FilterDefinition::getArgs
                            ))
                    );
                    return info;
                });
    }

    public Mono<Map<String, Map<String, String>>> getPredicates(String routeId) {
        return getRouteDefinition(routeId)
                .map(definition -> definition.getPredicates().stream()
                        .collect(Collectors.toMap(
                                PredicateDefinition::getName,
                                PredicateDefinition::getArgs
                        ))
                )
                .defaultIfEmpty(Collections.emptyMap());
    }
}
