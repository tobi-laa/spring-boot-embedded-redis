package io.github.tobi.laa.spring.boot.embedded.redis.junit.extension

import io.github.tobi.laa.spring.boot.embedded.redis.cluster.EmbeddedRedisCluster
import io.github.tobi.laa.spring.boot.embedded.redis.cluster.EmbeddedRedisCluster.Shard
import io.github.tobi.laa.spring.boot.embedded.redis.cluster.RedisShardCustomizer
import io.github.tobi.laa.spring.boot.embedded.redis.highavailability.EmbeddedRedisHighAvailability
import io.github.tobi.laa.spring.boot.embedded.redis.highavailability.EmbeddedRedisHighAvailability.Sentinel
import io.github.tobi.laa.spring.boot.embedded.redis.highavailability.RedisHighAvailabilityCustomizer
import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone
import io.github.tobi.laa.spring.boot.embedded.redis.standalone.RedisStandaloneCustomizer
import org.assertj.core.api.ListAssert
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.testkit.engine.EngineExecutionResults
import org.junit.platform.testkit.engine.EngineTestKit
import org.junit.platform.testkit.engine.Event
import org.junit.platform.testkit.engine.EventConditions.event
import org.junit.platform.testkit.engine.EventConditions.finishedWithFailure
import org.junit.platform.testkit.engine.TestExecutionResultConditions.message
import redis.embedded.core.RedisSentinelBuilder
import redis.embedded.core.RedisServerBuilder
import java.util.stream.Stream

@DisplayName("Testing the validations of embedded Redis annotations")
internal class RedisValidationExtensionTest {

    private lateinit var testKitBuilder: EngineTestKit.Builder
    private lateinit var results: EngineExecutionResults

    @ParameterizedTest(name = "Test class annotated with {0} should fail")
    @ArgumentsSource(ClassesWithMultipleAnnotationsProvider::class)
    fun classWithMoreThanOneAnnotation_executingTests_shouldFail(clazz: Class<*>) {
        givenTestClass(clazz)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Only one of @EmbeddedRedisStandalone, @EmbeddedRedisHighAvailability, @EmbeddedRedisCluster is allowed")
                    )
                )
            )
    }

    @ParameterizedTest(name = "@EmbeddedRedisStandalone with port {0} should fail")
    @ArgumentsSource(StandaloneWithInvalidPortProvider::class)
    fun standalone_invalidPort_executingTests_shouldFail(clazz: Class<*>) {
        givenTestClass(clazz)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Port must be in range 0..65535")
                    )
                )
            )
    }

    @Test
    @DisplayName("@EmbeddedRedisStandalone with both settings and configFile should fail")
    fun standalone_settingsAndConfigFile_executingTests_shouldFail() {
        givenTestClass(SettingsAndConfigFile::class.java)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Either 'configFile' or 'settings' can be set, but not both")
                    )
                )
            )
    }

    @Test
    @DisplayName("@EmbeddedRedisStandalone with customizer that does not have a no-args constructor should fail")
    fun standalone_customizerWithoutNoArgsConstructor_executingTests_shouldFail() {
        givenTestClass(StandaloneWithCustomizerWithoutNoArgsConstructor::class.java)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Customizers must have a no-arg constructor")
                    )
                )
            )
    }

    @Test
    @DisplayName("@EmbeddedRedisCluster with no shards should fail")
    fun cluster_noShards_executingTests_shouldFail() {
        givenTestClass(NoShards::class.java)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Shards must not be empty")
                    )
                )
            )
    }

    @ParameterizedTest(name = "@EmbeddedRedisCluster with {0} should fail")
    @ArgumentsSource(ClusterWithInvalidNOfReplicasProvider::class)
    fun cluster_invalidNOfReplicas_executingTests_shouldFail(clazz: Class<*>) {
        givenTestClass(clazz)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Replicas for all shards must be greater than 0")
                    )
                )
            )
    }

    @ParameterizedTest(name = "@EmbeddedRedisCluster with {0} should fail")
    @ArgumentsSource(ClusterWithInvalidNOfPortsProvider::class)
    fun cluster_invalidNOfPort_executingTests_shouldFail(clazz: Class<*>) {
        givenTestClass(clazz)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("If ports are specified, they must match the number of nodes")
                    )
                )
            )
    }

    @ParameterizedTest(name = "@EmbeddedRedisCluster with ports {0} should fail")
    @ArgumentsSource(ClusterWithInvalidPortsProvider::class)
    fun cluster_invalidPort_executingTests_shouldFail(clazz: Class<*>) {
        givenTestClass(clazz)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("All ports must be in range 0..65535")
                    )
                )
            )
    }

    @Test
    @DisplayName("@EmbeddedRedisCluster with duplicate ports should fail")
    fun cluster_portsSpecifiedMoreThanOnce_executingTests_shouldFail() {
        givenTestClass(ClusterWithDuplicatePorts::class.java)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Ports must not be specified more than once")
                    )
                )
            )
    }

    @ParameterizedTest(name = "@EmbeddedRedisCluster with initialization timeout {0} should fail")
    @ArgumentsSource(ClusterWithInvalidInitTimeoutProvider::class)
    fun cluster_invalidInitTimeout_executingTests_shouldFail(clazz: Class<*>) {
        givenTestClass(clazz)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Initialization timeout must be greater than 0")
                    )
                )
            )
    }

    @Test
    @DisplayName("@EmbeddedRedisCluster with customizer that does not have a no-args constructor should fail")
    fun cluster_customizerWithoutNoArgsConstructor_executingTests_shouldFail() {
        givenTestClass(ClusterWithCustomizerWithoutNoArgsConstructor::class.java)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Customizers must have a no-arg constructor")
                    )
                )
            )
    }

    @Test
    @DisplayName("@EmbeddedRedisHighAvailability with no sentinels should fail")
    fun highAvailability_noReplicationGroups_executingTests_shouldFail() {
        givenTestClass(NoSentinels::class.java)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Sentinels must not be empty")
                    )
                )
            )
    }

    @ParameterizedTest(name = "@EmbeddedRedisHighAvailability with {0} should fail")
    @ArgumentsSource(HighAvailabilityWithInvalidNOfReplicasProvider::class)
    fun highAvailability_invalidNOfReplicas_executingTests_shouldFail(clazz: Class<*>) {
        givenTestClass(clazz)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Replicas must be greater than 0")
                    )
                )
            )
    }

    @ParameterizedTest(name = "@EmbeddedRedisHighAvailability with {0} should fail")
    @ArgumentsSource(HighAvailabilityWithInvalidNOfPortsProvider::class)
    fun highAvailability_invalidNOfPorts_executingTests_shouldFail(clazz: Class<*>) {
        givenTestClass(clazz)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("If ports are specified, they must match the number of nodes")
                    )
                )
            )
    }

    @ParameterizedTest(name = "@EmbeddedRedisHighAvailability with ports {0} should fail")
    @ArgumentsSource(HighAvailabilityWithInvalidPortsProvider::class)
    fun highAvailability_invalidPorts_executingTests_shouldFail(clazz: Class<*>) {
        givenTestClass(clazz)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("All ports must be in range 0..65535")
                    )
                )
            )
    }

    @ParameterizedTest(name = "@EmbeddedRedisHighAvailability with {0} timeout for unreachable nodes should fail")
    @ArgumentsSource(HighAvailabilityWithInvalidDownAfterMillisProvider::class)
    fun highAvailability_invalidDownAfterMillis_executingTests_shouldFail(clazz: Class<*>) {
        givenTestClass(clazz)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Timeout for unreachable nodes must be greater than 0")
                    )
                )
            )
    }

    @ParameterizedTest(name = "@EmbeddedRedisHighAvailability with {0} failover timeout should fail")
    @ArgumentsSource(HighAvailabilityWithInvalidFailOverTimeoutMillisProvider::class)
    fun highAvailability_invalidFailOverTimeoutMillis_executingTests_shouldFail(clazz: Class<*>) {
        givenTestClass(clazz)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Failover timeout must be greater than 0")
                    )
                )
            )
    }

    @ParameterizedTest(name = "@EmbeddedRedisHighAvailability with {0} parallel syncs should fail")
    @ArgumentsSource(HighAvailabilityWithInvalidParallelSyncsProvider::class)
    fun highAvailability_invalidParallelSyncs_executingTests_shouldFail(clazz: Class<*>) {
        givenTestClass(clazz)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Parallel syncs must be greater than 0")
                    )
                )
            )
    }

    @Test
    @DisplayName("@EmbeddedRedisHighAvailability with customizer that does not have a no-args constructor should fail")
    fun highAvailability_customizerWithoutNoArgsConstructor_executingTests_shouldFail() {
        givenTestClass(HighAvailabilityWithCustomizerWithoutNoArgsConstructor::class.java)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Customizers must have a no-arg constructor")
                    )
                )
            )
    }

    @ParameterizedTest(name = "@EmbeddedRedisHighAvailability with {0} should fail")
    @ArgumentsSource(HighAvailabilityWithDuplicatePortsProvider::class)
    fun highAvailability_portsSpecifiedMoreThanOnce_executingTests_shouldFail(clazz: Class<*>) {
        givenTestClass(clazz)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Ports must not be specified more than once")
                    )
                )
            )
    }

    private fun givenTestClass(clazz: Class<*>) {
        testKitBuilder = EngineTestKit
            .engine("junit-jupiter")
            .selectors(selectClass(clazz))
    }

    private fun whenExecutingTests() {
        results = testKitBuilder.execute()
    }

    private fun thenEvents(): ListAssert<Event> {
        return results.containerEvents().assertThatEvents()
    }

    internal class ClassesWithMultipleAnnotationsProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("all three annotations", AllThreeAnnotations::class.java)),
                arguments(
                    named(
                        "@EmbeddedRedis and @EmbeddedRedisHighAvailability",
                        StandaloneAndHighAvail::class.java
                    )
                ),
                arguments(
                    named(
                        "@EmbeddedRedis and @EmbeddedRedisCluster",
                        StandaloneAndCluster::class.java
                    )
                ),
                arguments(
                    named(
                        "@EmbeddedRedisHighAvailability and @EmbeddedRedisCluster",
                        HighAvailAndCluster::class.java
                    )
                ),
                arguments(named("@EmbeddedRedis twice", StandaloneTwice::class.java)),
            )
        }

        @EmbeddedRedisStandalone
        @EmbeddedRedisHighAvailability
        @EmbeddedRedisCluster
        internal class AllThreeAnnotations : WithDummyTest()

        @EmbeddedRedisStandalone
        @EmbeddedRedisHighAvailability
        internal class StandaloneAndHighAvail : WithDummyTest()

        @EmbeddedRedisStandalone
        @EmbeddedRedisCluster
        internal class StandaloneAndCluster : WithDummyTest()

        @EmbeddedRedisHighAvailability
        @EmbeddedRedisCluster
        internal class HighAvailAndCluster : WithDummyTest()

        @EmbeddedRedisStandalone
        @SneakyBastard
        internal class StandaloneTwice : WithDummyTest()

        @Target(AnnotationTarget.CLASS)
        @Retention(AnnotationRetention.RUNTIME)
        @EmbeddedRedisStandalone
        annotation class SneakyBastard
    }

    internal class StandaloneWithInvalidPortProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("that is negative", NegativePort::class.java)),
                arguments(named("65536", Port65536::class.java)),
                arguments(named("65537", Port65537::class.java)),
            )
        }

        @EmbeddedRedisStandalone(port = -1)
        internal class NegativePort : WithDummyTest()

        @EmbeddedRedisStandalone(port = 65536)
        internal class Port65536 : WithDummyTest()

        @EmbeddedRedisStandalone(port = 65537)
        internal class Port65537 : WithDummyTest()
    }

    @EmbeddedRedisStandalone(settings = ["setting1", "setting2"], configFile = "configFile")
    internal class SettingsAndConfigFile : WithDummyTest()

    @EmbeddedRedisStandalone(customizer = [CustomizerWithoutNoArgsConstructor::class])
    internal class StandaloneWithCustomizerWithoutNoArgsConstructor : WithDummyTest()

    internal class CustomizerWithoutNoArgsConstructor(val sth: String) : RedisStandaloneCustomizer {
        override fun accept(builder: RedisServerBuilder, config: EmbeddedRedisStandalone) {
            // no-op
        }
    }

    @EmbeddedRedisCluster(shards = [])
    internal class NoShards : WithDummyTest()

    internal class ClusterWithInvalidNOfReplicasProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("a shard that has a negative number of replicas", NegativeReplicas::class.java)),
                arguments(named("a shard that has 0 replicas", ZeroReplicas::class.java))
            )
        }

        @EmbeddedRedisCluster(shards = [Shard(replicas = -1)])
        internal class NegativeReplicas : WithDummyTest()

        @EmbeddedRedisCluster(shards = [Shard(replicas = 0)])
        internal class ZeroReplicas : WithDummyTest()
    }

    internal class ClusterWithInvalidNOfPortsProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(
                    named(
                        "1 shard with 2 replicas but 2 ports (3 are needed)",
                        ThreeNodesButTwoPorts::class.java
                    )
                ),
                arguments(
                    named(
                        "2 shards with 1 replica but 3 ports (4 are needed)",
                        FourNodesButThreePorts::class.java
                    )
                )
            )
        }

        @EmbeddedRedisCluster(ports = [1, 2])
        internal class ThreeNodesButTwoPorts : WithDummyTest()

        @EmbeddedRedisCluster(shards = [Shard(replicas = 1), Shard(replicas = 1)], ports = [1, 2, 3])
        internal class FourNodesButThreePorts : WithDummyTest()
    }

    internal class ClusterWithInvalidPortsProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("that include a negative number", NegativePort::class.java)),
                arguments(named("that include 65536", Port65536::class.java))
            )
        }

        @EmbeddedRedisCluster(ports = [-1, 1, 2])
        internal class NegativePort : WithDummyTest()

        @EmbeddedRedisCluster(ports = [65534, 65535, 65536])
        internal class Port65536 : WithDummyTest()
    }

    @EmbeddedRedisCluster(ports = [1, 2, 2])
    internal class ClusterWithDuplicatePorts : WithDummyTest()

    internal class ClusterWithInvalidInitTimeoutProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("0", Zero::class.java)),
                arguments(named("-1", Negative::class.java))
            )
        }

        @EmbeddedRedisCluster(initializationTimeout = 0)
        internal class Zero : WithDummyTest()

        @EmbeddedRedisCluster(initializationTimeout = -1)
        internal class Negative : WithDummyTest()
    }

    @EmbeddedRedisCluster(customizer = [ClusterCustomizerWithoutNoArgsConstructor::class])
    internal class ClusterWithCustomizerWithoutNoArgsConstructor : WithDummyTest()

    internal class ClusterCustomizerWithoutNoArgsConstructor(val sth: String) : RedisShardCustomizer {

        override fun customizeMainNode(
            builder: RedisServerBuilder,
            config: EmbeddedRedisCluster,
            shard: String
        ) {
            // no-op
        }

        override fun customizeReplicas(
            builder: List<RedisServerBuilder>,
            config: EmbeddedRedisCluster,
            shard: String
        ) {
            // no-op
        }
    }

    @EmbeddedRedisHighAvailability(sentinels = [])
    internal class NoSentinels : WithDummyTest()

    internal class HighAvailabilityWithInvalidNOfReplicasProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("a negative number of replicas", NegativeReplicas::class.java)),
                arguments(named("0 replicas", ZeroReplicas::class.java))
            )
        }

        @EmbeddedRedisHighAvailability(replicas = -1)
        internal class NegativeReplicas : WithDummyTest()

        @EmbeddedRedisHighAvailability(replicas = 0)
        internal class ZeroReplicas : WithDummyTest()
    }

    internal class HighAvailabilityWithInvalidNOfPortsProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(
                    named(
                        "2 replicas but 2 ports (3 are needed)",
                        ThreeNodesButTwoPorts::class.java
                    )
                ),
                arguments(
                    named(
                        "1 replica but 3 ports (2 are needed)",
                        FourNodesButThreePorts::class.java
                    )
                )
            )
        }

        @EmbeddedRedisHighAvailability(ports = [1, 2])
        internal class ThreeNodesButTwoPorts : WithDummyTest()

        @EmbeddedRedisHighAvailability(replicas = 1, ports = [1, 2, 3])
        internal class FourNodesButThreePorts : WithDummyTest()
    }

    internal class HighAvailabilityWithInvalidPortsProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("that include a negative number", NegativePort::class.java)),
                arguments(named("that include a negative number for a sentinel", NegativePortSentinel::class.java)),
                arguments(named("that include 65536", Port65536::class.java)),
                arguments(named("that include 65536 for a sentinel", Port65536Sentinel::class.java))
            )
        }

        @EmbeddedRedisHighAvailability(ports = [-1, 1, 2])
        internal class NegativePort : WithDummyTest()

        @EmbeddedRedisHighAvailability(sentinels = [Sentinel(port = -1)])
        internal class NegativePortSentinel : WithDummyTest()

        @EmbeddedRedisHighAvailability(ports = [65534, 65535, 65536])
        internal class Port65536 : WithDummyTest()

        @EmbeddedRedisHighAvailability(sentinels = [Sentinel(port = 65536)])
        internal class Port65536Sentinel : WithDummyTest()
    }

    internal class HighAvailabilityWithInvalidDownAfterMillisProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("0 as", Zero::class.java)),
                arguments(named("a negative number as", Negative::class.java))
            )
        }

        @EmbeddedRedisHighAvailability(sentinels = [Sentinel(downAfterMillis = 0)])
        internal class Zero : WithDummyTest()

        @EmbeddedRedisHighAvailability(sentinels = [Sentinel(downAfterMillis = -1)])
        internal class Negative : WithDummyTest()
    }

    internal class HighAvailabilityWithInvalidFailOverTimeoutMillisProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("0 as", Zero::class.java)),
                arguments(named("a negative number as", Negative::class.java))
            )
        }

        @EmbeddedRedisHighAvailability(sentinels = [Sentinel(failOverTimeoutMillis = 0)])
        internal class Zero : WithDummyTest()

        @EmbeddedRedisHighAvailability(sentinels = [Sentinel(failOverTimeoutMillis = -1)])
        internal class Negative : WithDummyTest()
    }

    internal class HighAvailabilityWithInvalidParallelSyncsProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("0 as", Zero::class.java)),
                arguments(named("a negative number as", Negative::class.java))
            )
        }

        @EmbeddedRedisHighAvailability(sentinels = [Sentinel(parallelSyncs = 0)])
        internal class Zero : WithDummyTest()

        @EmbeddedRedisHighAvailability(sentinels = [Sentinel(parallelSyncs = -1)])
        internal class Negative : WithDummyTest()
    }

    @EmbeddedRedisHighAvailability(customizer = [HighAvailabilityCustomizerWithoutNoArgsConstructor::class])
    internal class HighAvailabilityWithCustomizerWithoutNoArgsConstructor : WithDummyTest()

    internal class HighAvailabilityCustomizerWithoutNoArgsConstructor(val sth: String) :
        RedisHighAvailabilityCustomizer {
        override fun customizeMainNode(builder: RedisServerBuilder, config: EmbeddedRedisHighAvailability) {
            // no-op
        }

        override fun customizeReplicas(builder: List<RedisServerBuilder>, config: EmbeddedRedisHighAvailability) {
            // no-op
        }

        override fun customizeSentinels(
            builder: RedisSentinelBuilder,
            config: EmbeddedRedisHighAvailability,
            sentinelConfig: Sentinel
        ) {
            // no-op
        }

    }

    internal class HighAvailabilityWithDuplicatePortsProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(
                    named(
                        "duplicate ports across nodes and a sentinel",
                        DupPortsGroupAndSentinel::class.java
                    )
                ),
                arguments(named("duplicate ports across two sentinels", DupPortsTwoSentinels::class.java)),
            )
        }

        @EmbeddedRedisHighAvailability(ports = [1, 2, 3], sentinels = [Sentinel(port = 1)])
        internal class DupPortsGroupAndSentinel : WithDummyTest()

        @EmbeddedRedisHighAvailability(sentinels = [Sentinel(port = 1), Sentinel(port = 1)])
        internal class DupPortsTwoSentinels : WithDummyTest()
    }

    internal abstract class WithDummyTest {
        @Test
        fun dummy() {
        }
    }
}