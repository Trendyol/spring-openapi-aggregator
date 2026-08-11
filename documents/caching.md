# Caching

The aggregator caches fetched service documents and the unified document. Caching is disabled by default.

Enable it with:

```yaml
openapi:
  aggregator:
    cache:
      enabled: true
      ttl: 10m
```

## Spring cache integration

If the application provides a Spring `CacheManager`, the aggregator uses it automatically. When multiple managers are available, Spring's default or `@Primary` manager is selected. A specific manager can be selected by its bean name:

```yaml
openapi:
  aggregator:
    cache:
      enabled: true
      cache-manager-name: primaryCacheManager
```

The cache region can also be selected explicitly:

```yaml
openapi:
  aggregator:
    cache:
      cache-name: gateway-primary-cache
```

When `cache-name` is omitted, the first named cache exposed by the selected `CacheManager` is used. If the manager does not expose a named cache, the aggregator uses its built-in cache name and the manager may create it dynamically.

The service and unified documents share the selected cache region and use separate key namespaces, so their entries do not conflict. Eviction and expiry behavior comes from the application-provided cache manager. The `ttl` property applies to the aggregator's built-in in-memory cache when no Spring `CacheManager` is available.

For example, a Caffeine-backed manager can be configured by the application:

```java
@Bean
@Primary
CacheManager primaryCacheManager() {
    CaffeineCacheManager manager = new CaffeineCacheManager("gateway-primary-cache");
    manager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(10)));
    return manager;
}
```
