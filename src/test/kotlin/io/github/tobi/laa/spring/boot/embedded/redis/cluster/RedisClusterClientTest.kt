package io.github.tobi.laa.spring.boot.embedded.redis.cluster

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import redis.clients.jedis.Connection
import redis.clients.jedis.ConnectionPool
import redis.clients.jedis.JedisCluster
import redis.clients.jedis.Protocol

@ExtendWith(MockKExtension::class)
@DisplayName("Tests for RedisClusterClient")
internal class RedisClusterClientTest {

    @InjectMockKs
    private lateinit var redisClusterClient: RedisClusterClient

    @RelaxedMockK
    private lateinit var jedisCluster: JedisCluster

    @Test
    @DisplayName("flushAll() should call flushAll() on JedisCluster")
    fun flushAll_shouldCallFlushAllOnJedisCluster() {
        val resources = listOf(mockk<Connection>("foo", true), mockk<Connection>("bar", true))
        val clusterNodes = mapOf(
            Pair("foo", mockk<ConnectionPool>()),
            Pair("bar", mockk<ConnectionPool>())
        )
        clusterNodes.values.forEachIndexed { index, connectionPool -> every { connectionPool.resource } returns resources[index] }
        every { jedisCluster.clusterNodes } returns clusterNodes
        redisClusterClient.flushAll()
        verify { resources.forEach { it.sendCommand(Protocol.Command.FLUSHALL) } }
    }

    @Test
    @DisplayName("get() should call get() on JedisCluster")
    fun get_shouldCallGetOnJedisCluster() {
        val key = "key"
        redisClusterClient.get(key)
        verify { jedisCluster.get(key) }
    }

    @Test
    @DisplayName("close() should call close() on JedisCluster")
    fun close_shouldCallCloseOnJedisCluster() {
        redisClusterClient.close()
        verify { jedisCluster.close() }
    }
}