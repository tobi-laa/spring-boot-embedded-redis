package io.github.tobi.laa.spring.boot.embedded.redis.junit.extension

import org.assertj.core.api.ListAssert
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.engine.discovery.DiscoverySelectors
import org.junit.platform.testkit.engine.EngineExecutionResults
import org.junit.platform.testkit.engine.EngineTestKit
import org.junit.platform.testkit.engine.Event
import org.junit.platform.testkit.engine.EventConditions
import org.junit.platform.testkit.engine.EventConditions.finishedWithFailure
import org.junit.platform.testkit.engine.TestExecutionResultConditions.message
import redis.embedded.RedisCluster
import redis.embedded.RedisServer
import redis.embedded.RedisShardedCluster

@DisplayName("Tests for RedisParameterResolver")
internal class RedisParameterResolverTest {

    private lateinit var testKitBuilder: EngineTestKit.Builder
    private lateinit var results: EngineExecutionResults

    @Test
    @DisplayName("Test class should fail if standalone Redis server is unresolvable")
    fun redisStandaloneUnresolvable_executingTestClass_shouldFail() {
        givenTestClass(RedisStandaloneUnresolvable::class.java)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                EventConditions.event(
                    finishedWithFailure(
                        message { it.matches(Regex("No ParameterResolver registered for parameter.+redis.embedded.RedisServer.+")) }
                    )
                )
            )
    }

    @Test
    @DisplayName("Test class should fail if Redis cluster is unresolvable")
    fun redisClusterUnresolvable_executingTestClass_shouldFail() {
        givenTestClass(RedisClusterUnresolvable::class.java)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                EventConditions.event(
                    finishedWithFailure(
                        message { it.matches(Regex("No ParameterResolver registered for parameter.+redis.embedded.RedisShardedCluster.+")) }
                    )
                )
            )
    }

    @Test
    @DisplayName("Test class should fail if high availability Redis server is unresolvable")
    fun redisHighAvailabilityUnresolvable_executingTestClass_shouldFail() {
        givenTestClass(RedisHighAvailabilityUnresolvable::class.java)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                EventConditions.event(
                    finishedWithFailure(
                        message { it.matches(Regex("No ParameterResolver registered for parameter.+redis.embedded.RedisCluster.+")) }
                    )
                )
            )
    }

    private fun givenTestClass(clazz: Class<*>) {
        testKitBuilder = EngineTestKit
            .engine("junit-jupiter")
            .selectors(DiscoverySelectors.selectClass(clazz))
    }

    private fun whenExecutingTests() {
        results = testKitBuilder.execute()
    }

    private fun thenEvents(): ListAssert<Event> {
        return results.allEvents().assertThatEvents()
    }

    @ExtendWith(RedisParameterResolver::class)
    internal class RedisStandaloneUnresolvable : WithDummyTest() {

        @BeforeEach
        fun dummy(redis: RedisServer) {
            // no-op
        }
    }

    @ExtendWith(RedisParameterResolver::class)
    internal class RedisClusterUnresolvable : WithDummyTest() {

        @BeforeEach
        fun dummy(redis: RedisShardedCluster) {
            // no-op
        }
    }

    @ExtendWith(RedisParameterResolver::class)
    internal class RedisHighAvailabilityUnresolvable : WithDummyTest() {

        @BeforeEach
        fun dummy(redis: RedisCluster) {
            // no-op
        }
    }

    internal abstract class WithDummyTest {
        @Test
        fun dummy() {
        }
    }
}