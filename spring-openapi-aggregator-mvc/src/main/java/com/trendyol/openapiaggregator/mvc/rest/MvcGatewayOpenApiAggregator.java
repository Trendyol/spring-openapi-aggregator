package com.trendyol.openapiaggregator.mvc.rest;

import com.trendyol.openapiaggregator.configuration.OpenApiOverridesConfig;
import com.trendyol.openapiaggregator.dto.ServiceApiDoc;
import com.trendyol.openapiaggregator.dto.ServiceRoute;
import com.trendyol.openapiaggregator.exception.OpenApiAggregatorException;
import com.trendyol.openapiaggregator.mvc.service.RouteDefinitionAccessor;
import com.trendyol.openapiaggregator.service.OpenApiAggregatorCacheService;
import com.trendyol.openapiaggregator.service.OpenApiRouteCustomizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/v3/api-docs")
@RequiredArgsConstructor
public class MvcGatewayOpenApiAggregator {
    private final RouteDefinitionAccessor routeDefinitionAccessor;
    private final OpenApiRouteCustomizer customizer;
    private final OpenApiOverridesConfig overrides;
    private final RestClient.Builder restClientBuilder;
    private final OpenApiAggregatorCacheService cache;

    @GetMapping("/{serviceName}")
    public String getServiceDocs(@PathVariable("serviceName") String serviceName) {
        return cache.getServiceDocument(serviceName).orElseGet(() -> {
            ServiceRoute route = route(serviceName);
            String document = customize(serviceName, fetch(route));
            cache.putServiceDocument(serviceName, document);
            return document;
        });
    }

    @GetMapping("/unified")
    public String getUnifiedDocs() {
        return cache.getUnifiedDocument().orElseGet(() -> {
            List<ServiceApiDoc> documents = routeDefinitionAccessor.getRoutes().stream()
                    .map(this::toServiceRoute)
                    .flatMap(Optional::stream)
                    .map(route -> {
                        try {
                            return Optional.of(new ServiceApiDoc(
                                    route.getServiceName(), customize(route.getServiceName(), fetch(route))));
                        } catch (OpenApiAggregatorException exception) {
                            log.warn("Failed to aggregate API docs for service {}: {}",
                                    route.getServiceName(), exception.getMessage(), exception);
                            return Optional.<ServiceApiDoc>empty();
                        }
                    }).flatMap(Optional::stream).toList();
            String document = customizer.mergeApiDocs(documents);
            cache.putUnifiedDocument(document);
            return document;
        });
    }

    private String customize(String serviceName, String document) {
        return customizer.applyOpenApiCustomisations(new ServiceApiDoc(serviceName, document),
                routeDefinitionAccessor.getPredicates(serviceName),
                routeDefinitionAccessor.getFilterConfigs(serviceName));
    }

    private String fetch(ServiceRoute route) {
        String body;
        try {
            body = restClientBuilder.baseUrl(route.getServiceUri()).build().get()
                    .uri(baseUrl(route.getServiceName())).retrieve().body(String.class);
        } catch (OpenApiAggregatorException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new OpenApiAggregatorException("Failed to fetch API documentation for service "
                    + route.getServiceName(), exception);
        }
        if (body == null) {
            throw new OpenApiAggregatorException("API documentation response body is missing for service "
                    + route.getServiceName());
        }
        return body;
    }

    private ServiceRoute route(String serviceName) {
        return routeDefinitionAccessor.getRoutes().stream().filter(route -> serviceName.equals(route.getId()))
                .findFirst().flatMap(this::toServiceRoute)
                .orElseThrow(() -> new OpenApiAggregatorException("Service route not found: " + serviceName));
    }

    private Optional<ServiceRoute> toServiceRoute(RouteProperties route) {
        String routeId = route.getId();
        URI routeUri = route.getUri();
        if (routeId == null || routeUri == null) {
            return Optional.empty();
        }
        return Optional.of(new ServiceRoute(routeId, routeUri.toString()));
    }

    private String baseUrl(String serviceName) {
        return overrides.getOverrides().getOrDefault(serviceName,
                new OpenApiOverridesConfig.ServiceOverrides()).getBaseUrl();
    }
}
