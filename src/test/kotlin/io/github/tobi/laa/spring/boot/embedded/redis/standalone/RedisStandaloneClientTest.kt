package io.github.tobi.laa.spring.boot.embedded.redis.standalone

import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import redis.clients.jedis.JedisPooled

@ExtendWith(MockKExtension::class)
@DisplayName("Tests for RedisStandaloneClient")
internal class RedisStandaloneClientTest {

    @InjectMockKs
    private lateinit var redisStandaloneClient: RedisStandaloneClient

    @RelaxedMockK
    private lateinit var jedisPooled: JedisPooled

    @Test
    @DisplayName("flushAll() should call flushAll() on JedisPooled")
    fun flushAll_shouldCallFlushAllOnJedisPooled() {
        redisStandaloneClient.flushAll()
        verify { jedisPooled.flushAll() }
    }

    @Test
    @DisplayName("get() should call get() on JedisPooled")
    fun get_shouldCallGetOnJedisPooled() {
        val key = "key"
        redisStandaloneClient.get(key)
        verify { jedisPooled.get(key) }
    }

    @Test
    @DisplayName("close() should call close() on JedisPooled")
    fun close_shouldCallCloseOnJedisPooled() {
        redisStandaloneClient.close()
        verify { jedisPooled.close() }
    }
}