package com.cadernosegredos.config;

import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class RedisConfig {
    private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);
    private static JedisPool jedisPool;
    private static final String REDIS_HOST = System.getenv().getOrDefault("REDIS_HOST", "localhost");
    private static final int REDIS_PORT = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));

    static {
        try {
            final JedisPoolConfig poolConfig = buildPoolConfig();
            jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT);
            logger.info("JedisPool inicializado com sucesso para {}:{}", REDIS_HOST, REDIS_PORT);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (jedisPool != null && !jedisPool.isClosed()) {
                    jedisPool.destroy();
                    logger.info("JedisPool fechado via shutdown hook.");
                }
            }));
        } catch (Exception e) {
            logger.error("Erro ao inicializar JedisPool: {}", e.getMessage(), e);
        }
    }

    private static JedisPoolConfig buildPoolConfig() {
        final JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        // --- CORREÇÃO DO MÉTODO DEPRECIADO ---
        // poolConfig.setMinEvictableIdleTime(Duration.ofSeconds(60)); // <-- Linha depreciada
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis()); // <-- Nova linha
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);
        return poolConfig;
    }

    public static JedisPool getJedisPool() {
        if (jedisPool == null || jedisPool.isClosed()) {
            logger.warn("JedisPool não está inicializado ou está fechado. Tentando re-inicializar...");
            try {
                final JedisPoolConfig poolConfig = buildPoolConfig();
                jedisPool = new JedisPool(poolConfig, REDIS_HOST, REDIS_PORT);
                logger.info("JedisPool re-inicializado.");
            } catch (Exception e) {
                logger.error("Falha ao re-inicializar JedisPool: {}", e.getMessage(), e);
                throw new RuntimeException("Falha ao obter JedisPool", e);
            }
        }
        return jedisPool;
    }

    public static void closeJedisPool() {
        if (jedisPool != null && !jedisPool.isClosed()) {
            jedisPool.destroy();
            logger.info("JedisPool fechado manualmente.");
        }
    }
}