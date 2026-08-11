package com.trendyol.openapiaggregator.service;

import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class TtlCacheTest {
    @Test
    void storesReadsAndEvictsEntries() {
        TtlCache cache = new TtlCache("documents", Duration.ofMinutes(1));

        cache.put("orders", "document");

        assertThat(cache.get("orders", String.class)).isEqualTo("document");
        assertThat(cache.putIfAbsent("orders", "replacement")).isNotNull();
        assertThat(cache.get("orders", String.class)).isEqualTo("document");

        cache.evict("orders");

        assertThat(cache.get("orders")).isNull();
    }

    @Test
    void expiresEntriesAfterTheirTtl() {
        TtlCache cache = new TtlCache("documents", Duration.ZERO);
        cache.put("orders", "document");

        assertThat(cache.get("orders")).isNull();
    }

    @Test
    void clearsAllEntries() {
        TtlCache cache = new TtlCache("documents", Duration.ofMinutes(1));
        cache.put("orders", "document");
        cache.put("reviews", "document");

        cache.clear();

        assertThat(cache.get("orders")).isNull();
        assertThat(cache.get("reviews")).isNull();
    }

    @Test
    void managerCreatesTtlCachesWithTheConfiguredExpiry() {
        TtlCacheManager manager = new TtlCacheManager(Duration.ZERO);

        Cache cache = manager.getCache("documents");

        assertThat(cache).isInstanceOf(TtlCache.class);
        assertThat(manager.getCacheNames()).contains("documents");
        cache.put("orders", "document");
        assertThat(cache.get("orders")).isNull();
    }
}
