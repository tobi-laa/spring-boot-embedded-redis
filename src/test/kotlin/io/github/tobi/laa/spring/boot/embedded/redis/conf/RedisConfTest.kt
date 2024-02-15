package io.github.tobi.laa.spring.boot.embedded.redis.conf

import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Tests for RedisConf")
internal class RedisConfTest {

    @Test
    @DisplayName("RedisConf should throw exception for blank keyword")
    fun blankKeyword_shouldThrowException() {
        assertThatThrownBy { RedisConf(listOf(RedisConf.Directive(""))) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("Keyword must not be blank")
    }

    @Test
    @DisplayName("RedisConf should throw exception for missing argument")
    fun missingArgument_shouldThrowException() {
        assertThatThrownBy { RedisConf(listOf(RedisConf.Directive("dummy"))) }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessage("At least one argument is required")
    }
}