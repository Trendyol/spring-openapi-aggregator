package com.trendyol.openapiaggregator.service;

import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import java.time.Duration;

final class TtlCacheManager extends ConcurrentMapCacheManager {
    private final Duration ttl;

    TtlCacheManager(Duration ttl) {
        this.ttl = ttl;
    }

    @Override
    protected Cache createConcurrentMapCache(String name) {
        return new TtlCache(name, ttl);
    }
}
