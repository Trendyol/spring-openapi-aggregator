package com.trendyol.openapiaggregator.rest;

import com.trendyol.openapiaggregator.configuration.OpenApiOverridesConfig;
import com.trendyol.openapiaggregator.dto.ServiceApiDoc;
import com.trendyol.openapiaggregator.dto.ServiceRoute;
import com.trendyol.openapiaggregator.service.OpenApiRouteCustomizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Slf4j
@RestController
@RequestMapping("/v3/api-docs")
@RequiredArgsConstructor
public class GatewayOpenApiAggregator {
    private final RouteDefinitionLocator routeDefinitionLocator;
    private final WebClient.Builder webClientBuilder;
    private final OpenApiRouteCustomizer openApiCustomizer;
    private final OpenApiOverridesConfig openApiOverridesConfig;

    @GetMapping("/{serviceName}")
    public Mono<String> getServiceDocs(@PathVariable("serviceName") String serviceName) {
        return fetchApiDocs(serviceName)
                .flatMap(apiDocsJson -> openApiCustomizer.applyOpenApiCustomisations(new ServiceApiDoc(serviceName, apiDocsJson)));
    }

    @GetMapping("/unified")
    public Mono<String> getUnifiedDocs() {
        return getServiceRoutes()
                .flatMap(this::fetchAndCustomizeServiceDocs)
                .collectList()
                .flatMap(openApiCustomizer::mergeApiDocs);
    }

    private Flux<ServiceRoute> getServiceRoutes() {
        return routeDefinitionLocator.getRouteDefinitions()
                .map(route -> new ServiceRoute(route.getId(), route.getUri().toString()));
    }

    private Mono<ServiceApiDoc> fetchAndCustomizeServiceDocs(ServiceRoute serviceRoute) {
        return webClientBuilder.build()
                .get()
                .uri(serviceRoute.getServiceUri() + getBaseUrl(serviceRoute.getServiceName()))
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(apiDocs -> openApiCustomizer.applyOpenApiCustomisations(new ServiceApiDoc(serviceRoute.getServiceName(), apiDocs)))
                .map(customizedDocs -> new ServiceApiDoc(serviceRoute.getServiceName(), customizedDocs))
                .onErrorResume(e -> {
                    log.warn("Failed to fetch API docs for service {}: {}", serviceRoute.getServiceName(), e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<String> getServiceUri(String serviceName) {
        return routeDefinitionLocator.getRouteDefinitions()
                .filter(route -> Objects.equals(route.getId(), serviceName))
                .next()
                .map(route -> Objects.requireNonNull(route.getUri()).toString())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Service route not found: " + serviceName)));
    }

    private String getBaseUrl(String serviceName) {
        return openApiOverridesConfig.getOverrides()
                .getOrDefault(serviceName, new OpenApiOverridesConfig.ServiceOverrides())
                .getBaseUrl();
    }

    private Mono<String> fetchApiDocs(String serviceName) {
        return getServiceUri(serviceName)
                .map(serviceUri -> serviceUri + getBaseUrl(serviceName))
                .flatMap(apiDocsUri -> webClientBuilder.build()
                        .get()
                        .uri(apiDocsUri)
                        .retrieve()
                        .bodyToMono(String.class));
    }
}
