package io.github.tobi.laa.spring.boot.embedded.redis.standalone

import io.github.tobi.laa.spring.boot.embedded.redis.RedisClient
import redis.clients.jedis.JedisPooled

internal class RedisStandaloneClient(private val jedisPooled: JedisPooled) : RedisClient {

    override fun flushAll(): String = jedisPooled.flushAll()

    override fun get(key: String): String = jedisPooled[key]

    override fun close(): Unit = jedisPooled.close()
}