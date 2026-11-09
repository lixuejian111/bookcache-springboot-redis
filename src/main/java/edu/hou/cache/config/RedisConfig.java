package edu.hou.cache.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CachingConfigurerSupport;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Redis全局配置类
 * 功能：自定义Redis序列化器，解决key/value乱码；配置缓存管理器、设置缓存过期时间
 * 开启Spring缓存注解支持 @EnableCaching
 */
@EnableCaching
@Configuration
public class RedisConfig extends CachingConfigurerSupport {

    // 缓存统一过期时间：600秒
    private static final long CACHE_TTL_SECONDS = 600L;

    /**
     * 自定义RedisTemplate，替换默认Jdk序列化，使用String+JacksonJson序列化
     * 解决Redis存储对象乱码问题
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        // 设置连接工厂
        redisTemplate.setConnectionFactory(factory);

        // 1. 创建字符串序列化器（key专用）
        RedisSerializer<String> stringSerializer = new StringRedisSerializer();
        // 2. 创建JSON序列化器（value专用）
        Jackson2JsonRedisSerializer<Object> jsonSerializer = buildJacksonSerializer();

        // 设置key序列化
        redisTemplate.setKeySerializer(stringSerializer);
        // 设置普通value序列化
        redisTemplate.setValueSerializer(jsonSerializer);
        // 设置Hash结构value序列化
        redisTemplate.setHashValueSerializer(jsonSerializer);

        // 初始化参数生效
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    /**
     * Redis缓存管理器，统一配置缓存过期、序列化、禁止缓存null值
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisSerializer<String> stringSerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<Object> jsonSerializer = buildJacksonSerializer();

        // 缓存全局配置
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                // 设置缓存有效期10分钟
                .entryTtl(Duration.ofSeconds(CACHE_TTL_SECONDS))
                // key使用字符串序列化
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(stringSerializer))
                // value使用JSON序列化
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                // 不缓存null数据，避免缓存穿透
                .disableCachingNullValues();

        // 构建缓存管理器
        return RedisCacheManager.builder(factory)
                .cacheDefaults(cacheConfig)
                .build();
    }

    /**
     * 抽取公共Jackson序列化构建方法，消除重复代码
     * 解决实体类序列化、反序列化转换异常问题
     */
    private Jackson2JsonRedisSerializer<Object> buildJacksonSerializer() {
        Jackson2JsonRedisSerializer<Object> jsonSerializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper();

        // 全部属性可见
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        // 新版jackson推荐方式，替代过时enableDefaultTyping
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL
        );

        jsonSerializer.setObjectMapper(objectMapper);
        return jsonSerializer;
    }
}