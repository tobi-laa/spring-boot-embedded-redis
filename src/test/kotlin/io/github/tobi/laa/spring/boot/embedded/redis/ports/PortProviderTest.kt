package io.github.tobi.laa.spring.boot.embedded.redis.ports

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.assertj.core.api.AbstractIntegerAssert
import org.assertj.core.api.Assertions.*
import org.assertj.core.api.ThrowableAssert
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.util.stream.IntStream.range

@ExtendWith(MockKExtension::class)
@DisplayName("PortProvider tests")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PortProviderTest {

    private lateinit var portProvider: PortProvider

    private var givenSentinel = false
    private var requestPorts: ThrowableAssert.ThrowingCallable? = null
    private var actualPorts = emptyList<Int>()

    @BeforeAll
    fun mockPortChecker() {
        mockkObject(PortChecker)
    }

    @AfterAll
    fun unmockPortChecker() {
        unmockkObject(PortChecker)
    }

    @BeforeEach
    fun setup() {
        portProvider = PortProvider()
        givenSentinel = false
        requestPorts = null
        actualPorts = emptyList<Int>().toMutableList()
    }

    @Test
    @DisplayName("Error should occur if no free port is found")
    fun noFreePorts_requestingPort_throwsError() {
        givenNoFreePorts()
        whenNextPortIsRequested()
        thenErrorIsThrown()
    }

    @Test
    @DisplayName("Valid port should be returned if all ports are free")
    fun freePorts_requestingPort_returnsValidPort() {
        givenFreePorts()
        whenNextPortIsRequested()
        thenValidPortIsReturned()
    }

    @Test
    @DisplayName("Valid sentinel port should be returned if all ports are free")
    fun freePorts_requestingPortForSentinel_returnsValidPort() {
        givenSentinel()
        givenFreePorts()
        whenNextPortIsRequested()
        thenValidPortIsReturned()
    }

    @Test
    @DisplayName("Valid port greater than 10000 should be returned if ports smaller than 10000 are taken")
    fun freePortsAfter10000_requestingPort_returnsValidPortGreater10000() {
        givenFreePortsStartingAt(10001)
        whenNextPortIsRequested()
        thenValidPortIsReturned().isGreaterThan(10000)
    }

    @DisplayName("Multiple valid ports should be returned if all ports are free")
    @ParameterizedTest(name = "{0} valid ports should be returned")
    @ValueSource(ints = [5, 10, 100, 10000])
    fun freePorts_requestingPorts_returnsValidPorts(nOfPorts: Int) {
        givenFreePorts()
        whenNextPortsAreRequested(nOfPorts)
        thenValidPortsAreReturned(nOfPorts)
    }

    private fun givenSentinel() {
        givenSentinel = true
    }

    private fun givenFreePorts() = givenFreePortsStartingAt(1)

    private fun givenFreePortsStartingAt(smallestFreePort: Int) {
        every { PortChecker.available(less(smallestFreePort)) } returns false
        every { PortChecker.available(more(smallestFreePort - 1)) } returns true
    }

    private fun givenNoFreePorts() {
        every { PortChecker.available(any()) } returns false
    }

    private fun whenNextPortIsRequested() = whenNextPortsAreRequested(1)

    private fun whenNextPortsAreRequested(nOfPorts: Int) {
        requestPorts = ThrowableAssert.ThrowingCallable {
            range(0, nOfPorts).forEach { _ -> actualPorts += portProvider.next(givenSentinel) }
        }
    }

    private fun thenErrorIsThrown() {
        assertThatThrownBy(requestPorts!!)
            .isExactlyInstanceOf(IllegalStateException::class.java)
            .hasMessage("Could not find an available TCP port")
    }

    private fun thenValidPortIsReturned(): AbstractIntegerAssert<*> {
        thenValidPortsAreReturned(1)
        return assertThat(actualPorts.first())
    }

    private fun thenValidPortsAreReturned(nOfPorts: Int) {
        assertThatCode(requestPorts!!).doesNotThrowAnyException()
        assertThat(actualPorts).hasSize(nOfPorts)
        assertThat(actualPorts).isNotEmpty.doesNotHaveDuplicates()
        if (givenSentinel) {
            assertThat(actualPorts).allSatisfy { assertThat(it).isBetween(26379, 65535) }
        } else {
            assertThat(actualPorts).allSatisfy { assertThat(it).isBetween(6379, 65535) }
        }
        // bus port should have been left free
        assertThat(actualPorts).allSatisfy { assertThat(actualPorts).doesNotContain(it + 10000) }
    }
}