package io.github.tobi.laa.spring.boot.embedded.redis.ports

import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.api.extension.ExtendWith
import java.io.IOException
import java.net.ServerSocket
import javax.net.ServerSocketFactory
import kotlin.random.Random

@ExtendWith(MockKExtension::class)
@DisplayName("PortChecker tests")
@TestInstance(PER_CLASS)
internal class PortCheckerTest {

    @MockK
    private lateinit var factory: ServerSocketFactory

    @MockK
    private lateinit var givenSocket: ServerSocket
    private var givenPort: Int = 0

    private var actualAvailable: Boolean = false

    @BeforeAll
    fun mockStatic() {
        mockkStatic(ServerSocketFactory::class)
        every { ServerSocketFactory.getDefault() } returns factory
    }

    @AfterAll
    fun closeStaticMock() {
        unmockkStatic(ServerSocketFactory::getDefault)
    }

    @RepeatedTest(5)
    @DisplayName("Port is unavailable if socket creation does not succeed")
    fun socketCreationError_whenCheckingPort_isUnvailable() {
        givenArbitraryPort()
        givenSocketCreationFails()
        whenCheckingPort()
        thenPortIsUnavailable()
        thenSocketCreationWasAttempted()
    }

    @RepeatedTest(5)
    @DisplayName("Port is available if socket creation succeeds and socket is closed afterwards")
    fun socketCreationSuccess_whenCheckingPort_isAvailable_closesSocket() {
        givenArbitraryPort()
        givenSocketCreationSucceeds()
        whenCheckingPort()
        thenPortIsAvailable()
        thenSocketCreationWasAttempted()
        thenSocketIsClosed()
    }

    private fun givenArbitraryPort() {
        givenPort = Random.Default.nextInt(1, 65535)
    }

    private fun givenSocketCreationSucceeds() {
        every { factory.createServerSocket(any(), any(), any()) } returns givenSocket
        every { givenSocket.close() } returns Unit
    }

    private fun givenSocketCreationFails() {
        every { factory.createServerSocket(any(), any(), any()) } throws IOException()
    }

    private fun whenCheckingPort() {
        actualAvailable = PortChecker.available(givenPort)
    }

    private fun thenPortIsUnavailable() {
        assertThat(actualAvailable).isFalse
    }

    private fun thenPortIsAvailable() {
        assertThat(actualAvailable).isTrue
    }

    private fun thenSocketCreationWasAttempted() {
        verify { factory.createServerSocket(eq(givenPort), any(), any()) }
    }

    private fun thenSocketIsClosed() {
        verify { givenSocket.close() }
    }
}
