package com.trendyol.openapiaggregator.service;

import com.trendyol.openapiaggregator.configuration.OpenApiCacheConfig;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;

import java.time.Duration;
import java.util.Optional;

public class OpenApiAggregatorCacheService {
    public static final String SERVICE_DOCUMENTS_CACHE = "openapi-aggregator-service-documents";
    private static final String UNIFIED_KEY = "unified";
    private static final String SERVICE_KEY_PREFIX = "service:";

    private final OpenApiCacheConfig config;
    private final CacheManager cacheManager;
    private final String cacheName;

    public OpenApiAggregatorCacheService(OpenApiCacheConfig config, ObjectProvider<CacheManager> cacheManagers,
                                         BeanFactory beanFactory) {
        this.config = config;
        CacheManager cacheManager = selectCacheManager(config, cacheManagers, beanFactory);
        this.cacheManager = cacheManager != null ? cacheManager : inMemoryCacheManager(config.getTtl());
        this.cacheName = selectCacheName(config, this.cacheManager);
    }

    private static @Nullable CacheManager selectCacheManager(OpenApiCacheConfig config,
                                                   ObjectProvider<CacheManager> cacheManagers,
                                                   BeanFactory beanFactory) {
        String managerName = config.getCacheManagerName();
        if (managerName != null && !managerName.isBlank()) {
            return beanFactory.getBean(managerName, CacheManager.class);
        }
        return cacheManagers.getIfAvailable();
    }

    private static String selectCacheName(OpenApiCacheConfig config, CacheManager cacheManager) {
        if (config.getCacheName() != null && !config.getCacheName().isBlank()) {
            return config.getCacheName();
        }
        return cacheManager.getCacheNames().stream()
                .findFirst()
                .orElse(SERVICE_DOCUMENTS_CACHE);
    }

    public static CacheManager inMemoryCacheManager(Duration ttl) {
        return new TtlCacheManager(ttl);
    }

    public Optional<String> getServiceDocument(String serviceName) {
        return Optional.ofNullable(get(cacheName, SERVICE_KEY_PREFIX + serviceName));
    }

    public void putServiceDocument(String serviceName, String document) {
        put(cacheName, SERVICE_KEY_PREFIX + serviceName, document);
    }

    public Optional<String> getUnifiedDocument() {
        return Optional.ofNullable(get(cacheName, UNIFIED_KEY));
    }

    public void putUnifiedDocument(String document) { put(cacheName, UNIFIED_KEY, document); }

    public void invalidate() {
        if (config.isEnabled()) {
            clear(cacheName);
        }
    }

    public void invalidateService(String serviceName) {
        if (!config.isEnabled()) {
            return;
        }
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(SERVICE_KEY_PREFIX + serviceName);
        }
        Cache unifiedCache = cacheManager.getCache(cacheName);
        if (unifiedCache != null) {
            unifiedCache.evict(UNIFIED_KEY);
        }
    }

    private @Nullable String get(String cacheName, String key) {
        if (!config.isEnabled()) {
            return null;
        }
        Cache cache = cacheManager.getCache(cacheName);
        return cache == null ? null : cache.get(key, String.class);
    }

    private void put(String cacheName, String key, String value) {
        if (config.isEnabled()) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.put(key, value);
            }
        }
    }

    private void clear(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
