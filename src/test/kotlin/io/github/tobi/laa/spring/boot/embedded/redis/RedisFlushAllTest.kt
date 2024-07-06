package io.github.tobi.laa.spring.boot.embedded.redis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("RedisFlushAll tests")
internal class RedisFlushAllTest {

    @Nested
    @DisplayName("RedisFlushAll.Mode tests")
    internal inner class ModeTest {

        @Test
        @DisplayName("Static field entries returns all enum values")
        fun getEntries_returnsAllValues() {
            assertThat(RedisFlushAll.Mode.entries).containsExactlyInAnyOrder(
                RedisFlushAll.Mode.AFTER_METHOD,
                RedisFlushAll.Mode.AFTER_CLASS,
                RedisFlushAll.Mode.NEVER
            )
        }
    }
}