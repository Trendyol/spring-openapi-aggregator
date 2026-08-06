package com.trendyol.openapiaggregator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServiceRoute {
    private String serviceName;
    private String serviceUri;
}
