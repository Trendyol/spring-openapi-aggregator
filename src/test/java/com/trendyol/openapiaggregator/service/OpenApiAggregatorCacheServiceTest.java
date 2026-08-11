package com.trendyol.openapiaggregator.service;

import com.trendyol.openapiaggregator.configuration.OpenApiCacheConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenApiAggregatorCacheServiceTest {
    private static final String SERVICE_NAME = "orders";
    private static final String DOCUMENT = "service-document";

    @Test
    void doesNotReadOrWriteWhenCachingIsDisabled() {
        CacheManager cacheManager = new ConcurrentMapCacheManager("gateway-cache");
        OpenApiAggregatorCacheService service = service(new OpenApiCacheConfig(), cacheManager);

        service.putServiceDocument(SERVICE_NAME, DOCUMENT);

        assertThat(service.getServiceDocument(SERVICE_NAME)).isEmpty();
        assertThat(cacheManager.getCache("gateway-cache").get("service:" + SERVICE_NAME)).isNull();
    }

    @Test
    void usesTheFirstSpringCacheWhenNoCacheNameIsConfigured() {
        OpenApiCacheConfig config = enabledConfig();
        CacheManager cacheManager = new ConcurrentMapCacheManager("gateway-cache");
        OpenApiAggregatorCacheService service = service(config, cacheManager);

        service.putServiceDocument(SERVICE_NAME, DOCUMENT);
        service.putUnifiedDocument("unified-document");

        assertThat(cacheManager.getCache("gateway-cache").get("service:" + SERVICE_NAME, String.class))
                .isEqualTo(DOCUMENT);
        assertThat(service.getServiceDocument(SERVICE_NAME)).contains(DOCUMENT);
        assertThat(service.getUnifiedDocument()).contains("unified-document");
    }

    @Test
    void usesTheConfiguredManagerAndCacheName() {
        OpenApiCacheConfig config = enabledConfig();
        config.setCacheManagerName("secondaryCacheManager");
        config.setCacheName("selected-cache");

        CacheManager primary = new ConcurrentMapCacheManager("primary-cache");
        CacheManager secondary = new ConcurrentMapCacheManager("selected-cache");
        ObjectProvider<CacheManager> managers = mock(ObjectProvider.class);
        BeanFactory beanFactory = mock(BeanFactory.class);
        when(managers.getIfAvailable()).thenReturn(primary);
        when(beanFactory.getBean("secondaryCacheManager", CacheManager.class)).thenReturn(secondary);

        OpenApiAggregatorCacheService service = new OpenApiAggregatorCacheService(config, managers, beanFactory);
        service.putServiceDocument(SERVICE_NAME, DOCUMENT);

        assertThat(secondary.getCache("selected-cache").get("service:" + SERVICE_NAME, String.class))
                .isEqualTo(DOCUMENT);
        assertThat(primary.getCache("primary-cache").get("service:" + SERVICE_NAME)).isNull();
    }

    @Test
    void invalidatesOneServiceAndTheUnifiedDocument() {
        OpenApiAggregatorCacheService service = service(enabledConfig(),
                new ConcurrentMapCacheManager("gateway-cache"));
        service.putServiceDocument(SERVICE_NAME, DOCUMENT);
        service.putUnifiedDocument("unified-document");

        service.invalidateService(SERVICE_NAME);

        assertThat(service.getServiceDocument(SERVICE_NAME)).isEmpty();
        assertThat(service.getUnifiedDocument()).isEmpty();
    }

    @Test
    void invalidatesAllCachedDocuments() {
        OpenApiAggregatorCacheService service = service(enabledConfig(),
                new ConcurrentMapCacheManager("gateway-cache"));
        service.putServiceDocument(SERVICE_NAME, DOCUMENT);
        service.putUnifiedDocument("unified-document");

        service.invalidate();

        assertThat(service.getServiceDocument(SERVICE_NAME)).isEmpty();
        assertThat(service.getUnifiedDocument()).isEmpty();
    }

    private static OpenApiCacheConfig enabledConfig() {
        OpenApiCacheConfig config = new OpenApiCacheConfig();
        config.setEnabled(true);
        return config;
    }

    private static OpenApiAggregatorCacheService service(OpenApiCacheConfig config,
                                                         CacheManager cacheManager) {
        ObjectProvider<CacheManager> managers = mock(ObjectProvider.class);
        when(managers.getIfAvailable()).thenReturn(cacheManager);
        return new OpenApiAggregatorCacheService(config, managers, mock(BeanFactory.class));
    }
}
