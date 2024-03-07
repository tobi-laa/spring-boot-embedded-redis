package io.github.tobi.laa.spring.boot.embedded.redis

import io.github.netmikey.logunit.api.LogCapturer
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.slf4j.event.Level
import redis.embedded.Redis

@ExtendWith(MockKExtension::class)
@DisplayName("Tests for stopSafely")
internal class StopSafelyTest {

    @RelaxedMockK
    private lateinit var redis: Redis

    @RegisterExtension
    val logs: LogCapturer =
        LogCapturer.create().captureForLogger("io.github.tobi.laa.spring.boot.embedded.redis", Level.DEBUG)

    @Test
    @DisplayName("stopSafely() should call stop() on the given Redis")
    fun stopSafely_shouldCallCloseOnGivenAutoCloseable() {
        stopSafely(redis)
        verify { redis.stop() }
        logs.assertDoesNotContain("Failed to stop Redis $redis")
    }

    @Test
    @DisplayName("stopSafely() should log an error if stop() on the given Redis throws an exception")
    fun stopSafely_shouldLogErrorIfStopThrowsException() {
        val exception = RuntimeException("close() failed")
        every { redis.stop() } throws exception
        stopSafely(redis)
        verify { redis.stop() }
        val logEvent = logs.assertContains("Failed to stop Redis $redis")
        assertThat(logEvent.throwable).isSameAs(exception)
    }
}