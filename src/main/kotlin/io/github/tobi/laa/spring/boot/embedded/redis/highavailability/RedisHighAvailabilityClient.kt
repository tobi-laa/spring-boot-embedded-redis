package io.github.tobi.laa.spring.boot.embedded.redis.highavailability

import io.github.tobi.laa.spring.boot.embedded.redis.RedisClient
import redis.clients.jedis.JedisSentinelPool

internal class RedisHighAvailabilityClient(private val jedisSentinelPool: JedisSentinelPool) : RedisClient,
    AutoCloseable by jedisSentinelPool {

    override fun flushAll(): String = jedisSentinelPool.resource.flushAll()

    override fun get(key: String): String = jedisSentinelPool.resource[key]
}