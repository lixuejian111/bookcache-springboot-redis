package edu.hou.cache.service.impl;

import edu.hou.cache.service.CacheService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class CacheServiceImpl implements CacheService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void saveToCache(String key, Object value) {
        // 设置过期时间，例如 1 小时
        redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);
    }

    @Override
    public Object getFromCache(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public void deleteFromCache(String key) {
        redisTemplate.delete(key);
    }
}