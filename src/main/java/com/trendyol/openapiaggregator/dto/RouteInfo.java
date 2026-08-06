package com.trendyol.openapiaggregator.dto;

import lombok.Data;

import java.util.Map;

@Data
public class RouteInfo {
    private String id;
    private String uri;
    private Map<String, Map<String, String>> predicates;
    private Map<String, Map<String, String>> filters;
}
