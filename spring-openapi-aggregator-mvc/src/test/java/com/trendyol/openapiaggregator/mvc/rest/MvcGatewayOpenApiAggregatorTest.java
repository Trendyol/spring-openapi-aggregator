package com.trendyol.openapiaggregator.mvc.rest;

import com.trendyol.openapiaggregator.configuration.OpenApiOverridesConfig;
import com.trendyol.openapiaggregator.mvc.service.RouteDefinitionAccessor;
import com.trendyol.openapiaggregator.service.OpenApiRouteCustomizer;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.server.mvc.config.RouteProperties;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MvcGatewayOpenApiAggregatorTest {
    private static final String ORDERS = "orders";
    private static final String ORDERS_URI = "http://orders";

    @Test
    void fetchesAndCustomizesAServiceDocument() {
        RouteProperties route = mock(RouteProperties.class);
        when(route.getId()).thenReturn(ORDERS);
        when(route.getUri()).thenReturn(URI.create(ORDERS_URI));

        RouteDefinitionAccessor routeAccessor = mock(RouteDefinitionAccessor.class);
        when(routeAccessor.getRoutes()).thenReturn(List.of(route));
        when(routeAccessor.getPredicates(ORDERS)).thenReturn(Map.of());
        when(routeAccessor.getFilterConfigs(ORDERS)).thenReturn(Map.of());

        OpenApiRouteCustomizer customizer = mock(OpenApiRouteCustomizer.class);
        when(customizer.applyOpenApiCustomisations(any(), any(), any())).thenReturn("customized");

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo(ORDERS_URI + "/v3/api-docs"))
                .andRespond(withSuccess("{\"openapi\":\"3.0.1\",\"paths\":{}}", MediaType.APPLICATION_JSON));

        MvcGatewayOpenApiAggregator aggregator = new MvcGatewayOpenApiAggregator(
                routeAccessor, customizer, new OpenApiOverridesConfig(), builder);

        assertThat(aggregator.getServiceDocs(ORDERS)).isEqualTo("customized");
        server.verify();
    }
}
