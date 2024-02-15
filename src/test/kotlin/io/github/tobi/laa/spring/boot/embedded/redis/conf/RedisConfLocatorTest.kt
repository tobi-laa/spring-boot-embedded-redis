package io.github.tobi.laa.spring.boot.embedded.redis.conf

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import redis.embedded.RedisServer

@DisplayName("Tests for RedisConfLocator")
internal class RedisConfLocatorTest {

    @Test
    @DisplayName("RedisConfLocator should throw exception for missing file")
    fun missingFile_shouldThrowException() {
        val redis = RedisServer(6379, emptyList(), false)
        assertThatThrownBy { RedisConfLocator.locate(redis) }
            .isExactlyInstanceOf(IllegalStateException::class.java)
            .hasMessage("No config file found for embedded Redis server: $redis")
    }
}