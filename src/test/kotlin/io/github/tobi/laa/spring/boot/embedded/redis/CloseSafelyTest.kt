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

@ExtendWith(MockKExtension::class)
@DisplayName("Tests for closeSafely")
internal class CloseSafelyTest {

    @RelaxedMockK
    private lateinit var closeable: AutoCloseable

    @RegisterExtension
    val logs: LogCapturer =
        LogCapturer.create().captureForLogger("io.github.tobi.laa.spring.boot.embedded.redis", Level.DEBUG)

    @Test
    @DisplayName("closeSafely() should call close() on the given AutoCloseable")
    fun closeSafely_shouldCallCloseOnGivenAutoCloseable() {
        closeSafely(closeable)
        verify { closeable.close() }
        logs.assertDoesNotContain("Failed to close $closeable")
    }

    @Test
    @DisplayName("closeSafely() should log an error if close() on the given AutoCloseable throws an exception")
    fun closeSafely_shouldLogErrorIfCloseThrowsException() {
        val exception = RuntimeException("close() failed")
        every { closeable.close() } throws exception
        closeSafely(closeable)
        verify { closeable.close() }
        val logEvent = logs.assertContains("Failed to close $closeable")
        assertThat(logEvent.throwable).isSameAs(exception)
    }
}