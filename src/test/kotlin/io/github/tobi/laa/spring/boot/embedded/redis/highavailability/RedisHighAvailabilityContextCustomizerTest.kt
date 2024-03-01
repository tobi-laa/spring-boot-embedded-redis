package io.github.tobi.laa.spring.boot.embedded.redis.highavailability

import io.github.tobi.laa.spring.boot.embedded.redis.RedisClient
import io.github.tobi.laa.spring.boot.embedded.redis.RedisStore
import io.github.tobi.laa.spring.boot.embedded.redis.ports.PortProvider
import io.mockk.impl.annotations.RelaxedMockK
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

@DisplayName("Tests for RedisHighAvailabilityContextCustomizer")
@ExtendWith(MockKExtension::class)
internal class RedisHighAvailabilityContextCustomizerTest {

    @RelaxedMockK
    private lateinit var portProvider: PortProvider

    private val config = EmbeddedRedisHighAvailability()

    private lateinit var givenCustomizer: RedisHighAvailabilityContextCustomizer

    @BeforeEach
    fun init() {
        givenCustomizer = RedisHighAvailabilityContextCustomizer(config, portProvider)
    }


    @Test
    @DisplayName("RedisHighAvailabilityContextCustomizer.equals() should be true for itself")
    fun sameObject_equals_shouldBeTrue() {
        val comparedToSelf = givenCustomizer.equals(givenCustomizer)
        assertThat(comparedToSelf).isTrue()
    }

    @Test
    @DisplayName("RedisHighAvailabilityContextCustomizer.equals() should be false for null objects")
    fun null_equals_shouldBeFalse() {
        val comparedToDifferentClass = givenCustomizer.equals(null)
        assertThat(comparedToDifferentClass).isFalse()
    }

    @Test
    @DisplayName("RedisHighAvailabilityContextCustomizer.equals() should be false for object with different class")
    fun differentClass_equals_shouldBeFalse() {
        val comparedToDifferentClass = givenCustomizer.equals("I'm not a RedisServerContextCustomizer")
        assertThat(comparedToDifferentClass).isFalse()
    }

    @Test
    @DisplayName("Closing ApplicationContext should stop Redis server and Redis client")
    fun closingApplicationContext_shouldStopRedisServerAndRedisClient() {
        var server: Redis?
        var client: RedisClient?
        AnnotationConfigApplicationContext().use {
            RedisHighAvailabilityContextCustomizerFactory()
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

    @EmbeddedRedisHighAvailability
    private class AnnotatedClass
}