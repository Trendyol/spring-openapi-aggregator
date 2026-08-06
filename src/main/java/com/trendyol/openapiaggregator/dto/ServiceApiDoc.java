package com.trendyol.openapiaggregator.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ServiceApiDoc {
    private String serviceName;
    private String apiDocs;
}
