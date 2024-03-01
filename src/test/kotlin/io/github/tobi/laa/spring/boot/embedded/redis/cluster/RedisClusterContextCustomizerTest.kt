package io.github.tobi.laa.spring.boot.embedded.redis.cluster

import io.github.tobi.laa.spring.boot.embedded.redis.RedisClient
import io.github.tobi.laa.spring.boot.embedded.redis.RedisStore
import io.github.tobi.laa.spring.boot.embedded.redis.ports.PortProvider
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import redis.embedded.Redis

@DisplayName("Tests for RedisClusterContextCustomizer")
@ExtendWith(MockKExtension::class)
internal class RedisClusterContextCustomizerTest {

    @MockK
    private lateinit var portProvider: PortProvider

    private val config = EmbeddedRedisCluster()

    private lateinit var givenCustomizer: RedisClusterContextCustomizer

    @BeforeEach
    fun init() {
        givenCustomizer = RedisClusterContextCustomizer(config, portProvider)
    }


    @Test
    @DisplayName("RedisClusterContextCustomizer.equals() should be true for itself")
    fun sameObject_equals_shouldBeTrue() {
        val comparedToSelf = givenCustomizer.equals(givenCustomizer)
        assertThat(comparedToSelf).isTrue()
    }

    @Test
    @DisplayName("RedisClusterContextCustomizer.equals() should be false for null objects")
    fun null_equals_shouldBeFalse() {
        val comparedToDifferentClass = givenCustomizer.equals(null)
        assertThat(comparedToDifferentClass).isFalse()
    }

    @Test
    @DisplayName("RedisClusterContextCustomizer.equals() should be false for object with different class")
    fun differentClass_equals_shouldBeFalse() {
        val comparedToDifferentClass = givenCustomizer.equals("I'm not a RedisClusterContextCustomizer")
        assertThat(comparedToDifferentClass).isFalse()
    }

    @Test
    @DisplayName("Closing ApplicationContext should stop Redis server and Redis client")
    fun closingApplicationContext_shouldStopRedisServerAndRedisClient() {
        var server: Redis?
        var client: RedisClient?
        AnnotationConfigApplicationContext().use {
            RedisClusterContextCustomizerFactory()
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

    @EmbeddedRedisCluster
    private class AnnotatedClass
}