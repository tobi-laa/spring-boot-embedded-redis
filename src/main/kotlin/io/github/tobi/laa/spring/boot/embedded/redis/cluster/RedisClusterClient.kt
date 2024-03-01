package io.github.tobi.laa.spring.boot.embedded.redis.cluster

import io.github.tobi.laa.spring.boot.embedded.redis.RedisClient
import redis.clients.jedis.JedisCluster
import redis.clients.jedis.Protocol

internal class RedisClusterClient(private val jedisCluster: JedisCluster) : RedisClient {

    override fun flushAll(): String {
        jedisCluster.clusterNodes.values.forEach { it.resource.sendCommand(Protocol.Command.FLUSHALL) }
        return ""
    }

    override fun get(key: String): String = jedisCluster[key]

    override fun close(): Unit = jedisCluster.close()
}