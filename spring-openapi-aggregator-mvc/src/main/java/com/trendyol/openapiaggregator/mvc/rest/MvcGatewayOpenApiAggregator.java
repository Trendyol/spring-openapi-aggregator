package com.trendyol.openapiaggregator.mvc.rest;

import com.trendyol.openapiaggregator.configuration.OpenApiOverridesConfig;
import com.trendyol.openapiaggregator.dto.ServiceApiDoc;
import com.trendyol.openapiaggregator.dto.ServiceRoute;
import com.trendyol.openapiaggregator.mvc.service.RouteDefinitionAccessor;
import com.trendyol.openapiaggregator.service.OpenApiRouteCustomizer;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/v3/api-docs")
@RequiredArgsConstructor
public class MvcGatewayOpenApiAggregator {
    private final RouteDefinitionAccessor routeDefinitionAccessor;
    private final OpenApiRouteCustomizer customizer;
    private final OpenApiOverridesConfig overrides;
    private final RestClient.Builder restClientBuilder;

    @GetMapping("/{serviceName}")
    public String getServiceDocs(@PathVariable("serviceName") String serviceName) {
        ServiceRoute route = route(serviceName);
        return customize(serviceName, fetch(route));
    }

    @GetMapping("/unified")
    public String getUnifiedDocs() {
        List<ServiceApiDoc> documents = routeDefinitionAccessor.getRoutes().stream()
                .map(this::toServiceRoute)
                .flatMap(Optional::stream)
                .map(route -> {
                    try {
                        return Optional.of(new ServiceApiDoc(
                                route.getServiceName(), customize(route.getServiceName(), fetch(route))));
                    } catch (RuntimeException exception) {
                        return Optional.<ServiceApiDoc>empty();
                    }
                }).flatMap(Optional::stream).toList();
        return customizer.mergeApiDocs(documents);
    }

    private String customize(String serviceName, String document) {
        return customizer.applyOpenApiCustomisations(new ServiceApiDoc(serviceName, document),
                routeDefinitionAccessor.getPredicates(serviceName),
                routeDefinitionAccessor.getFilterConfigs(serviceName));
    }

    private String fetch(ServiceRoute route) {
        return Objects.requireNonNull(
                restClientBuilder.baseUrl(route.getServiceUri()).build().get()
                        .uri(baseUrl(route.getServiceName())).retrieve().body(String.class),
                "API documentation response body must not be null");
    }

    private ServiceRoute route(String serviceName) {
        return routeDefinitionAccessor.getRoutes().stream().filter(route -> serviceName.equals(route.getId()))
                .findFirst().flatMap(this::toServiceRoute)
                .orElseThrow(() -> new IllegalArgumentException("Service route not found: " + serviceName));
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
