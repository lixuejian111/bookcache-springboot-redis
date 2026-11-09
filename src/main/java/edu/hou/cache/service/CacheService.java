package edu.hou.cache.service;

public interface CacheService {
    void saveToCache(String key, Object value);
    Object getFromCache(String key);
    void deleteFromCache(String key);
}