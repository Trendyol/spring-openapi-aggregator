package com.trendyol.openapiaggregator.service;

import java.util.Map;

public interface GatewayRouteAccessor {
    Map<String, Map<String, String>> getPredicates(String routeId);

    Map<String, Object> getFilterConfigs(String routeId);
}
