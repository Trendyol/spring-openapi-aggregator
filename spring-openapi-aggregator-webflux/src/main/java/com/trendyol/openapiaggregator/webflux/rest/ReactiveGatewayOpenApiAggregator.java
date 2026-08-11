package com.trendyol.openapiaggregator.webflux.rest;

import com.trendyol.openapiaggregator.configuration.OpenApiOverridesConfig;
import com.trendyol.openapiaggregator.dto.ServiceApiDoc;
import com.trendyol.openapiaggregator.dto.ServiceRoute;
import com.trendyol.openapiaggregator.exception.OpenApiAggregatorException;
import com.trendyol.openapiaggregator.service.OpenApiAggregatorCacheService;
import com.trendyol.openapiaggregator.service.OpenApiRouteCustomizer;
import com.trendyol.openapiaggregator.webflux.service.RouteDefinitionAccessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
@RestController
@RequestMapping("/v3/api-docs")
public class ReactiveGatewayOpenApiAggregator {
    private final RouteDefinitionLocator routeDefinitionLocator;
    private final RouteDefinitionAccessor routeDefinitionAccessor;
    private final WebClient.Builder webClientBuilder;
    private final OpenApiRouteCustomizer openApiCustomizer;
    private final OpenApiOverridesConfig openApiOverridesConfig;
    private final OpenApiAggregatorCacheService cache;

    @GetMapping("/{serviceName}")
    public Mono<String> getServiceDocs(@PathVariable("serviceName") String serviceName) {
        return Mono.justOrEmpty(cache.getServiceDocument(serviceName))
                .switchIfEmpty(fetchApiDocs(serviceName).flatMap(apiDocs -> customize(serviceName, apiDocs)))
                .doOnNext(document -> cache.putServiceDocument(serviceName, document));
    }

    @GetMapping("/unified")
    public Mono<String> getUnifiedDocs() {
        return Mono.justOrEmpty(cache.getUnifiedDocument())
                .switchIfEmpty(getServiceRoutes().flatMap(this::fetchAndCustomizeServiceDocs)
                        .collectList().map(openApiCustomizer::mergeApiDocs)
                        .doOnNext(cache::putUnifiedDocument));
    }

    private Flux<ServiceRoute> getServiceRoutes() {
        return routeDefinitionLocator.getRouteDefinitions()
                .flatMap(this::toServiceRoute);
    }

    private Mono<ServiceApiDoc> fetchAndCustomizeServiceDocs(ServiceRoute route) {
        return webClientBuilder.build().get()
                .uri(route.getServiceUri() + getBaseUrl(route.getServiceName()))
                .retrieve().bodyToMono(String.class)
                .flatMap(apiDocs -> customize(route.getServiceName(), apiDocs))
                .map(customized -> new ServiceApiDoc(route.getServiceName(), customized))
                .onErrorResume(error -> {
                    log.warn("Failed to fetch API docs for service {}: {}", route.getServiceName(), error.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<String> customize(String serviceName, String apiDocs) {
        return Mono.zip(routeDefinitionAccessor.getPredicates(serviceName),
                        routeDefinitionAccessor.getFilterConfigs(serviceName))
                .map(routeData -> openApiCustomizer.applyOpenApiCustomisations(
                        new ServiceApiDoc(serviceName, apiDocs), routeData.getT1(), routeData.getT2()));
    }

    private Mono<String> fetchApiDocs(String serviceName) {
        return routeDefinitionLocator.getRouteDefinitions()
                .filter(route -> Objects.equals(route.getId(), serviceName)).next()
                .flatMap(route -> {
                    URI routeUri = route.getUri();
                    if (routeUri == null) {
                        return Mono.error(new OpenApiAggregatorException("Service route has no URI: " + serviceName));
                    }
                    return Mono.just(routeUri + getBaseUrl(serviceName));
                })
                .switchIfEmpty(Mono.error(new OpenApiAggregatorException("Service route not found: " + serviceName)))
                .flatMap(uri -> webClientBuilder.build().get().uri(uri).retrieve().bodyToMono(String.class)
                        .switchIfEmpty(Mono.error(new OpenApiAggregatorException(
                                "API documentation response body is missing for service " + serviceName))))
                .onErrorMap(error -> error instanceof OpenApiAggregatorException
                        ? error
                        : new OpenApiAggregatorException("Failed to fetch API documentation for service "
                        + serviceName, error));
    }

    private Mono<ServiceRoute> toServiceRoute(RouteDefinition route) {
        String routeId = route.getId();
        URI routeUri = route.getUri();
        if (routeId == null || routeUri == null) {
            return Mono.empty();
        }
        return Mono.just(new ServiceRoute(routeId, routeUri.toString()));
    }

    private String getBaseUrl(String serviceName) {
        return openApiOverridesConfig.getOverrides()
                .getOrDefault(serviceName, new OpenApiOverridesConfig.ServiceOverrides()).getBaseUrl();
    }
}
