package io.github.tobi.laa.spring.boot.embedded.redis.highavailability

import io.github.tobi.laa.spring.boot.embedded.redis.RedisClient
import io.github.tobi.laa.spring.boot.embedded.redis.RedisStore
import io.github.tobi.laa.spring.boot.embedded.redis.highavailability.RedisHighAvailabilityContextCustomizer.Node
import io.github.tobi.laa.spring.boot.embedded.redis.highavailability.RedisHighAvailabilityContextCustomizer.NodeProvider
import io.github.tobi.laa.spring.boot.embedded.redis.ports.PortProvider
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkConstructor
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
        val comparedToDifferentClass = givenCustomizer.equals("I'm not a RedisHighAvailabilityContextCustomizer")
        assertThat(comparedToDifferentClass).isFalse()
    }

    @Test
    @DisplayName("RedisHighAvailabilityContextCustomizer should throw NoSuchElementException when no nodes are available")
    fun noMoreNodes_shouldThrow() {
        mockkConstructor(NodeProvider::class) {
            every { anyConstructed<NodeProvider>().next() } throws NoSuchElementException()
            assertThatThrownBy {
                AnnotationConfigApplicationContext().use {
                    RedisHighAvailabilityContextCustomizerFactory()
                        .createContextCustomizer(AnnotatedClass::class.java, mutableListOf())
                        .customizeContext(it, mockk())
                    it.refresh()
                    it.start()
                }
            }.isInstanceOf(NoSuchElementException::class.java)
        }
    }

    @Test
    @DisplayName("RedisHighAvailabilityContextCustomizer.NodeProvider should provide expected nodes")
    fun nodeProvider_shouldProvideExpectedNodes() {
        val ports = listOf(1, 2, 3)
        val binds = listOf("foo", "bar", "baz")
        val nodeProvider = NodeProvider(ports, binds)
        val actual = ports.indices.map { nodeProvider.next() }
        val expected = ports.zip(binds).map { (port, bind) -> Node(port, bind) }
        assertThat(actual).containsExactlyElementsOf(expected)
    }

    @Test
    @DisplayName("RedisHighAvailabilityContextCustomizer.NodeProvider should throw NoSuchElementException when no more ports are available")
    fun nodeProvider_noMorePorts_shouldThrow() {
        val ports = listOf(1)
        val binds = listOf("foo", "bar", "baz")
        val nodeProvider = NodeProvider(ports, binds)
        val actual = ports.indices.map { nodeProvider.next() }
        val expected = ports.zip(binds).map { (port, bind) -> Node(port, bind) }
        assertThat(actual).containsExactlyElementsOf(expected)
        assertThatThrownBy { nodeProvider.next() }.isInstanceOf(NoSuchElementException::class.java)
    }

    @Test
    @DisplayName("RedisHighAvailabilityContextCustomizer.NodeProvider should throw NoSuchElementException when no more binds are available")
    fun nodeProvider_noMoreBinds_shouldThrow() {
        val ports = listOf(1, 2, 3)
        val binds = listOf("foo")
        val nodeProvider = NodeProvider(ports, binds)
        val actual = binds.indices.map { nodeProvider.next() }
        val expected = ports.zip(binds).map { (port, bind) -> Node(port, bind) }
        assertThat(actual).containsExactlyElementsOf(expected)
        assertThatThrownBy { nodeProvider.next() }.isInstanceOf(NoSuchElementException::class.java)
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

    @EmbeddedRedisHighAvailability(ports = [0, 0, 0])
    private class AnnotatedClass
}