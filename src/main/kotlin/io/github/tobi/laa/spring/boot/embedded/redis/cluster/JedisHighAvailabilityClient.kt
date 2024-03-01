package io.github.tobi.laa.spring.boot.embedded.redis.cluster

import io.github.tobi.laa.spring.boot.embedded.redis.RedisClient
import redis.clients.jedis.JedisSentinelPool

internal class JedisHighAvailabilityClient(private val jedisSentinelPool: JedisSentinelPool) : RedisClient {

    override fun flushAll(): String = jedisSentinelPool.resource.flushAll()

    override fun get(key: String): String = jedisSentinelPool.resource[key]

    override fun close(): Unit = jedisSentinelPool.close()
}