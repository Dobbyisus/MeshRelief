package com.meshrelief.core.store;

import java.util.LinkedHashMap;
import java.util.Map;

public class SeenCache {

    private final int maxSize;
    //LRU Cache implementation
    private final LinkedHashMap<String, Boolean> cache;

    public SeenCache(int maxSize) {
        this.maxSize = maxSize;

        this.cache = new LinkedHashMap<String, Boolean>(maxSize, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > SeenCache.this.maxSize;
            }
        };
    }

    public synchronized boolean hasSeen(String packetId) {
        return cache.containsKey(packetId);
    }

    public synchronized void markSeen(String packetId) {
        cache.put(packetId, Boolean.TRUE);
    }

    // Optional: current size (for debugging)
    public synchronized int size() {
        return cache.size();
    }
}