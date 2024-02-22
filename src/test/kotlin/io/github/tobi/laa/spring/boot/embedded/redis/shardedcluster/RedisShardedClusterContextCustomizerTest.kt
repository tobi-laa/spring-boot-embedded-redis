package io.github.tobi.laa.spring.boot.embedded.redis.shardedcluster

import io.github.tobi.laa.spring.boot.embedded.redis.RedisStore
import io.github.tobi.laa.spring.boot.embedded.redis.ports.PortProvider
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import redis.clients.jedis.UnifiedJedis
import redis.embedded.Redis

@DisplayName("Tests for RedisShardedClusterContextCustomizer")
internal class RedisShardedClusterContextCustomizerTest {

    private val givenCustomizer =
        RedisShardedClusterContextCustomizer(mockk<EmbeddedRedisShardedCluster>(), mockk<PortProvider>())

    @Test
    @DisplayName("RedisShardedClusterContextCustomizer.equals() should be true for itself")
    fun sameObject_equals_shouldBeTrue() {
        val comparedToSelf = givenCustomizer.equals(givenCustomizer)
        assertThat(comparedToSelf).isTrue()
    }

    @Test
    @DisplayName("RedisShardedClusterContextCustomizer.equals() should be false for null objects")
    fun null_equals_shouldBeFalse() {
        val comparedToDifferentClass = givenCustomizer.equals(null)
        assertThat(comparedToDifferentClass).isFalse()
    }

    @Test
    @DisplayName("RedisShardedClusterContextCustomizer.equals() should be false for object with different class")
    fun differentClass_equals_shouldBeFalse() {
        val comparedToDifferentClass = givenCustomizer.equals("I'm not a RedisServerContextCustomizer")
        assertThat(comparedToDifferentClass).isFalse()
    }

    @Test
    @DisplayName("Closing ApplicationContext should stop Redis server and Redis client")
    fun closingApplicationContext_shouldStopRedisServerAndRedisClient() {
        var server: Redis? = null
        var client: UnifiedJedis? = null
        AnnotationConfigApplicationContext().use {
            RedisShardedClusterContextCustomizerFactory()
                .createContextCustomizer(AnnotatedClass::class.java, mutableListOf())
                .customizeContext(it, mockk())
            it.refresh()
            it.start()
            server = RedisStore.server(it)
            client = RedisStore.client(it)
        }
        assertThat(server!!.isActive).isFalse()
        assertThatThrownBy { client!!.get("FOO") }.isInstanceOf(Exception::class.java)
    }

    @EmbeddedRedisShardedCluster
    private class AnnotatedClass
}