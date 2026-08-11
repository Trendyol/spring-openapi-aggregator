package com.trendyol.openapiaggregator.configuration;

import lombok.Data;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "openapi.aggregator.cache")
@Data
public class OpenApiCacheConfig {
    private boolean enabled;
    private Duration ttl = Duration.ofMinutes(10);
    private @Nullable String cacheManagerName;
    private @Nullable String cacheName;
}
