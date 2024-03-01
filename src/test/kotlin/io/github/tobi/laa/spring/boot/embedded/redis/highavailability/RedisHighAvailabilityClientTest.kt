package io.github.tobi.laa.spring.boot.embedded.redis.highavailability

import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisSentinelPool

@ExtendWith(MockKExtension::class)
@DisplayName("Tests for RedisHighAvailabilityClient")
internal class RedisHighAvailabilityClientTest {

    @InjectMockKs
    private lateinit var redisHighAvailabilityClient: RedisHighAvailabilityClient

    @RelaxedMockK
    private lateinit var jedisSentinelPool: JedisSentinelPool

    @RelaxedMockK
    private lateinit var jedis: Jedis

    @BeforeEach
    internal fun mockPool() {
        every { jedisSentinelPool.resource } returns jedis
    }

    @Test
    @DisplayName("flushAll() should call flushAll() on JedisSentinelPool")
    fun flushAll_shouldCallFlushAllOnJedisSentinelPool() {
        redisHighAvailabilityClient.flushAll()
        verify { jedis.flushAll() }
    }

    @Test
    @DisplayName("get() should call get() on JedisSentinelPool")
    fun get_shouldCallGetOnJedisSentinelPool() {
        val key = "key"
        redisHighAvailabilityClient.get(key)
        verify { jedis.get(key) }
    }

    @Test
    @DisplayName("close() should call close() on JedisSentinelPool")
    fun close_shouldCallCloseOnJedisSentinelPool() {
        redisHighAvailabilityClient.close()
        verify { jedisSentinelPool.close() }
    }
}