package com.trendyol.openapiaggregator.mvc.rest;

import com.trendyol.openapiaggregator.configuration.OpenApiOverridesConfig;
import com.trendyol.openapiaggregator.dto.ServiceApiDoc;
import com.trendyol.openapiaggregator.dto.ServiceRoute;
import com.trendyol.openapiaggregator.mvc.service.RouteDefinitionAccessor;
import com.trendyol.openapiaggregator.service.OpenApiRouteCustomizer;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.List;

@RestController
@RequestMapping("/v3/api-docs")
@RequiredArgsConstructor
public class GatewayOpenApiAggregator {
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
                .map(route -> new ServiceRoute(route.getId(), route.getUri().toString()))
                .map(route -> {
                    try {
                        return new ServiceApiDoc(route.getServiceName(), customize(route.getServiceName(), fetch(route)));
                    } catch (RuntimeException exception) {
                        return null;
                    }
                }).filter(java.util.Objects::nonNull).toList();
        return customizer.mergeApiDocs(documents);
    }

    private String customize(String serviceName, String document) {
        return customizer.applyOpenApiCustomisations(new ServiceApiDoc(serviceName, document),
                routeDefinitionAccessor.getPredicates(serviceName),
                routeDefinitionAccessor.getFilterConfigs(serviceName));
    }

    private String fetch(ServiceRoute route) {
        return restClientBuilder.baseUrl(route.getServiceUri()).build().get()
                .uri(baseUrl(route.getServiceName())).retrieve().body(String.class);
    }

    private ServiceRoute route(String serviceName) {
        return routeDefinitionAccessor.getRoutes().stream().filter(route -> serviceName.equals(route.getId()))
                .findFirst().map(route -> new ServiceRoute(route.getId(), route.getUri().toString()))
                .orElseThrow(() -> new IllegalArgumentException("Service route not found: " + serviceName));
    }

    private String baseUrl(String serviceName) {
        return overrides.getOverrides().getOrDefault(serviceName,
                new OpenApiOverridesConfig.ServiceOverrides()).getBaseUrl();
    }
}
