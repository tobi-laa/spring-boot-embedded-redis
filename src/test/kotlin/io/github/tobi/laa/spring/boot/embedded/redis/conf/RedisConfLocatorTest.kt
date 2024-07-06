package io.github.tobi.laa.spring.boot.embedded.redis.conf

import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import redis.embedded.RedisInstance
import java.nio.file.Paths

@DisplayName("Tests for RedisConfLocator")
@ExtendWith(MockKExtension::class)
internal class RedisConfLocatorTest {

    @Test
    @DisplayName("RedisConfLocator should throw exception for empty args")
    fun emptyArgs_shouldThrowException() {
        val redis = TestRedis(emptyList())
        assertThatThrownBy { RedisConfLocator.locate(redis) }
            .isExactlyInstanceOf(IllegalStateException::class.java)
            .hasMessage("No config file found for embedded Redis server: $redis")
    }

    @Test
    @DisplayName("RedisConfLocator should throw exception for missing file with .conf extension")
    fun missingFile_shouldThrowException() {
        val redis = TestRedis(listOf("redis.properties"))
        assertThatThrownBy { RedisConfLocator.locate(redis) }
            .isExactlyInstanceOf(IllegalStateException::class.java)
            .hasMessage("No config file found for embedded Redis server: $redis")
    }

    @Test
    @DisplayName("RedisConfLocator should return path to redis.conf file if it exists")
    fun redisConfFound_shouldReturnPath() {
        val redis = TestRedis(listOf("redis.conf"))
        assertThat(RedisConfLocator.locate(redis)).isEqualTo(Paths.get("redis.conf"))
    }

    private class TestRedis(args: List<String>) : RedisInstance(0, args, null, false, null, null) {

        override fun start() {}
        override fun stop() {}
        override fun ports(): MutableList<Int> = mutableListOf()
        override fun isActive(): Boolean = false
    }
}