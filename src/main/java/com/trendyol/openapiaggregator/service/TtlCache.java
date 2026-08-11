package com.trendyol.openapiaggregator.service;

import org.jspecify.annotations.Nullable;
import org.springframework.cache.concurrent.ConcurrentMapCache;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

final class TtlCache extends ConcurrentMapCache {
    private final Duration ttl;
    private final ConcurrentMap<Object, Entry> entries = new ConcurrentHashMap<>();

    TtlCache(String name, Duration ttl) {
        super(name);
        this.ttl = ttl;
    }

    @Override
    public @Nullable ValueWrapper get(Object key) {
        Entry entry = entries.get(key);
        if (entry == null || entry.expired()) {
            if (entry != null) {
                entries.remove(key, entry);
            }
            return null;
        }
        return () -> entry.value;
    }

    @Override
    public <T> @Nullable T get(Object key, Class<T> type) {
        ValueWrapper value = get(key);
        return value == null ? null : type.cast(value.get());
    }

    @Override
    public void put(Object key, Object value) {
        entries.put(key, new Entry(value, ttl));
    }

    @Override
    public @Nullable ValueWrapper putIfAbsent(Object key, Object value) {
        ValueWrapper existing = get(key);
        if (existing != null) {
            return existing;
        }
        put(key, value);
        return null;
    }

    @Override
    public void evict(Object key) {
        entries.remove(key);
    }

    @Override
    public void clear() {
        entries.clear();
    }

    private record Entry(Object value, long expiresAt) {
        private Entry(Object value, Duration ttl) {
            this(value, System.nanoTime() + ttl.toNanos());
        }

        private boolean expired() {
            return System.nanoTime() >= expiresAt;
        }
    }
}
