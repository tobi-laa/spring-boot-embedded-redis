package io.github.tobi.laa.spring.boot.embedded.redis.junit.extension

import io.github.tobi.laa.spring.boot.embedded.redis.cluster.EmbeddedRedisCluster
import io.github.tobi.laa.spring.boot.embedded.redis.cluster.EmbeddedRedisCluster.ReplicationGroup
import io.github.tobi.laa.spring.boot.embedded.redis.cluster.EmbeddedRedisCluster.Sentinel
import io.github.tobi.laa.spring.boot.embedded.redis.cluster.RedisClusterCustomizer
import io.github.tobi.laa.spring.boot.embedded.redis.server.EmbeddedRedisServer
import io.github.tobi.laa.spring.boot.embedded.redis.server.RedisServerCustomizer
import io.github.tobi.laa.spring.boot.embedded.redis.shardedcluster.EmbeddedRedisShardedCluster
import io.github.tobi.laa.spring.boot.embedded.redis.shardedcluster.EmbeddedRedisShardedCluster.Shard
import io.github.tobi.laa.spring.boot.embedded.redis.shardedcluster.RedisShardCustomizer
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
                        message("Only one of @EmbeddedRedisServer, @EmbeddedRedisCluster, @EmbeddedRedisShardedCluster is allowed")
                    )
                )
            )
    }

    @ParameterizedTest(name = "@EmbeddedRedisServer with port {0} should fail")
    @ArgumentsSource(ServerWithInvalidPortProvider::class)
    fun embeddedRedis_invalidPort_executingTests_shouldFail(clazz: Class<*>) {
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
    @DisplayName("@EmbeddedRedisServer with both settings and configFile should fail")
    fun embeddedRedis_settingsAndConfigFile_executingTests_shouldFail() {
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
    @DisplayName("@EmbeddedRedisServer with customizer that does not have a no-args constructor should fail")
    fun embeddedRedis_customizerWithoutNoArgsConstructor_executingTests_shouldFail() {
        givenTestClass(ServerWithCustomizerWithoutNoArgsConstructor::class.java)
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
    @DisplayName("@EmbeddedRedisShardedCluster with no shards should fail")
    fun embeddedRedisShardedCluster_noShards_executingTests_shouldFail() {
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

    @ParameterizedTest(name = "@EmbeddedRedisShardedCluster with {0} should fail")
    @ArgumentsSource(ShardedClusterWithInvalidNOfReplicasProvider::class)
    fun embeddedRedisShardedCluster_invalidNOfReplicas_executingTests_shouldFail(clazz: Class<*>) {
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

    @ParameterizedTest(name = "@EmbeddedRedisShardedCluster with {0} should fail")
    @ArgumentsSource(ShardedClusterWithInvalidNOfPortsProvider::class)
    fun embeddedRedisShardedCluster_invalidNOfPort_executingTests_shouldFail(clazz: Class<*>) {
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

    @ParameterizedTest(name = "@EmbeddedRedisShardedCluster with ports {0} should fail")
    @ArgumentsSource(ShardedClusterWithInvalidPortsProvider::class)
    fun embeddedRedisShardedCluster_invalidPort_executingTests_shouldFail(clazz: Class<*>) {
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
    @DisplayName("@EmbeddedRedisShardedCluster with duplicate ports should fail")
    fun embeddedRedisShardedCluster_portsSpecifiedMoreThanOnce_executingTests_shouldFail() {
        givenTestClass(ShardedClusterWithDuplicatePorts::class.java)
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

    @ParameterizedTest(name = "@EmbeddedRedisShardedCluster with initialization timeout {0} should fail")
    @ArgumentsSource(ShardedClusterWithInvalidInitTimeoutProvider::class)
    fun embeddedRedisShardedCluster_invalidInitTimeout_executingTests_shouldFail(clazz: Class<*>) {
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
    @DisplayName("@EmbeddedRedisShardedCluster with customizer that does not have a no-args constructor should fail")
    fun embeddedRedisShardedCluster_customizerWithoutNoArgsConstructor_executingTests_shouldFail() {
        givenTestClass(ShardedClusterWithCustomizerWithoutNoArgsConstructor::class.java)
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
    @DisplayName("@EmbeddedRedisCluster with no replication groups should fail")
    fun embeddedRedisCluster_noReplicationGroups_executingTests_shouldFail() {
        givenTestClass(NoReplicationGroups::class.java)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Replication groups must not be empty")
                    )
                )
            )
    }

    @ParameterizedTest(name = "@EmbeddedRedisCluster with {0} should fail")
    @ArgumentsSource(ClusterWithInvalidNOfReplicasProvider::class)
    fun embeddedRedisCluster_invalidNOfReplicas_executingTests_shouldFail(clazz: Class<*>) {
        givenTestClass(clazz)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("Replicas for all replication groups must be greater than 0")
                    )
                )
            )
    }

    @ParameterizedTest(name = "@EmbeddedRedisCluster with {0} should fail")
    @ArgumentsSource(ClusterWithInvalidNOfPortsProvider::class)
    fun embeddedRedisCluster_invalidNOfPorts_executingTests_shouldFail(clazz: Class<*>) {
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
    fun embeddedRedisCluster_invalidPorts_executingTests_shouldFail(clazz: Class<*>) {
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
    @DisplayName("@EmbeddedRedisCluster with sentinel with unknown monitored group should fail")
    fun embeddedRedisCluster_sentinelWithUnknownMonitoredGroup_executingTests_shouldFail() {
        givenTestClass(SentinelWithUnknownMonitoredGroup::class.java)
        whenExecutingTests()
        thenEvents()
            .haveAtLeastOne(
                event(
                    finishedWithFailure(
                        message("All monitored groups must be present in a replication group")
                    )
                )
            )
    }

    @Test
    @DisplayName("@EmbeddedRedisCluster with customizer that does not have a no-args constructor should fail")
    fun embeddedRedisCluster_customizerWithoutNoArgsConstructor_executingTests_shouldFail() {
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

    @ParameterizedTest(name = "@EmbeddedRedisCluster with {0} should fail")
    @ArgumentsSource(ClusterWithDuplicatePortsProvider::class)
    fun embeddedRedisCluster_portsSpecifiedMoreThanOnce_executingTests_shouldFail(clazz: Class<*>) {
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
                arguments(named("@EmbeddedRedis and @EmbeddedRedisCluster", StandaloneAndCluster::class.java)),
                arguments(
                    named(
                        "@EmbeddedRedis and @EmbeddedRedisShardedCluster",
                        StandaloneAndShardedCluster::class.java
                    )
                ),
                arguments(
                    named(
                        "@EmbeddedRedisCluster and @EmbeddedRedisShardedCluster",
                        ClusterAndShardedCluster::class.java
                    )
                ),
                arguments(named("@EmbeddedRedis twice", StandaloneTwice::class.java)),
            )
        }

        @EmbeddedRedisServer
        @EmbeddedRedisCluster
        @EmbeddedRedisShardedCluster
        internal class AllThreeAnnotations : WithDummyTest()

        @EmbeddedRedisServer
        @EmbeddedRedisCluster
        internal class StandaloneAndCluster : WithDummyTest()

        @EmbeddedRedisServer
        @EmbeddedRedisShardedCluster
        internal class StandaloneAndShardedCluster : WithDummyTest()

        @EmbeddedRedisCluster
        @EmbeddedRedisShardedCluster
        internal class ClusterAndShardedCluster : WithDummyTest()

        @EmbeddedRedisServer
        @SneakyBastard
        internal class StandaloneTwice : WithDummyTest()

        @Target(AnnotationTarget.CLASS)
        @Retention(AnnotationRetention.RUNTIME)
        @EmbeddedRedisServer
        annotation class SneakyBastard
    }

    internal class ServerWithInvalidPortProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("that is negative", NegativePort::class.java)),
                arguments(named("65536", Port65536::class.java)),
                arguments(named("65537", Port65537::class.java)),
            )
        }

        @EmbeddedRedisServer(port = -1)
        internal class NegativePort : WithDummyTest()

        @EmbeddedRedisServer(port = 65536)
        internal class Port65536 : WithDummyTest()

        @EmbeddedRedisServer(port = 65537)
        internal class Port65537 : WithDummyTest()
    }

    @EmbeddedRedisServer(settings = ["setting1", "setting2"], configFile = "configFile")
    internal class SettingsAndConfigFile : WithDummyTest()

    @EmbeddedRedisServer(customizer = [CustomizerWithoutNoArgsConstructor::class])
    internal class ServerWithCustomizerWithoutNoArgsConstructor : WithDummyTest()

    internal class CustomizerWithoutNoArgsConstructor(val sth: String) : RedisServerCustomizer {
        override fun accept(builder: RedisServerBuilder, config: EmbeddedRedisServer) {
            // no-op
        }
    }

    @EmbeddedRedisShardedCluster(shards = [])
    internal class NoShards : WithDummyTest()

    internal class ShardedClusterWithInvalidNOfReplicasProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("a shard that has a negative number of replicas", NegativeReplicas::class.java)),
                arguments(named("a shard that has 0 replicas", ZeroReplicas::class.java))
            )
        }

        @EmbeddedRedisShardedCluster(shards = [Shard(replicas = -1)])
        internal class NegativeReplicas : WithDummyTest()

        @EmbeddedRedisShardedCluster(shards = [Shard(replicas = 0)])
        internal class ZeroReplicas : WithDummyTest()
    }

    internal class ShardedClusterWithInvalidNOfPortsProvider : ArgumentsProvider {

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

        @EmbeddedRedisShardedCluster(ports = [1, 2])
        internal class ThreeNodesButTwoPorts : WithDummyTest()

        @EmbeddedRedisShardedCluster(shards = [Shard(replicas = 1), Shard(replicas = 1)], ports = [1, 2, 3])
        internal class FourNodesButThreePorts : WithDummyTest()
    }

    internal class ShardedClusterWithInvalidPortsProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("that include a negative number", NegativePort::class.java)),
                arguments(named("that include 65536", Port65536::class.java))
            )
        }

        @EmbeddedRedisShardedCluster(ports = [-1, 1, 2])
        internal class NegativePort : WithDummyTest()

        @EmbeddedRedisShardedCluster(ports = [65534, 65535, 65536])
        internal class Port65536 : WithDummyTest()
    }

    @EmbeddedRedisShardedCluster(ports = [1, 2, 2])
    internal class ShardedClusterWithDuplicatePorts : WithDummyTest()

    internal class ShardedClusterWithInvalidInitTimeoutProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("0", Zero::class.java)),
                arguments(named("-1", Negative::class.java))
            )
        }

        @EmbeddedRedisShardedCluster(initializationTimeout = 0)
        internal class Zero : WithDummyTest()

        @EmbeddedRedisShardedCluster(initializationTimeout = -1)
        internal class Negative : WithDummyTest()
    }

    @EmbeddedRedisShardedCluster(customizer = [ShardedClusterCustomizerWithoutNoArgsConstructor::class])
    internal class ShardedClusterWithCustomizerWithoutNoArgsConstructor : WithDummyTest()

    internal class ShardedClusterCustomizerWithoutNoArgsConstructor(val sth: String) : RedisShardCustomizer {

        override fun customizeMainNode(
            builder: RedisServerBuilder,
            config: EmbeddedRedisShardedCluster,
            shard: String
        ) {
            // no-op
        }

        override fun customizeReplicas(
            builder: List<RedisServerBuilder>,
            config: EmbeddedRedisShardedCluster,
            shard: String
        ) {
            // no-op
        }
    }

    @EmbeddedRedisCluster(replicationGroups = [])
    internal class NoReplicationGroups : WithDummyTest()

    internal class ClusterWithInvalidNOfReplicasProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("a group with a negative number of replicas", NegativeReplicas::class.java)),
                arguments(named("a group with 0 replicas", ZeroReplicas::class.java))
            )
        }

        @EmbeddedRedisCluster(replicationGroups = [ReplicationGroup(replicas = -1)])
        internal class NegativeReplicas : WithDummyTest()

        @EmbeddedRedisCluster(replicationGroups = [ReplicationGroup(replicas = 0)])
        internal class ZeroReplicas : WithDummyTest()
    }

    internal class ClusterWithInvalidNOfPortsProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(
                    named(
                        "1 group with 2 replicas but 2 ports (3 are needed)",
                        ThreeNodesButTwoPorts::class.java
                    )
                ),
                arguments(
                    named(
                        "2 groups with 1 replica but the second one has 3 ports (2 are needed)",
                        FourNodesButThreePorts::class.java
                    )
                )
            )
        }

        @EmbeddedRedisCluster(replicationGroups = [ReplicationGroup(ports = [1, 2])])
        internal class ThreeNodesButTwoPorts : WithDummyTest()

        @EmbeddedRedisCluster(
            replicationGroups = [
                ReplicationGroup(replicas = 1, ports = [1, 2]),
                ReplicationGroup(replicas = 1, ports = [3, 4, 5])]
        )
        internal class FourNodesButThreePorts : WithDummyTest()
    }

    internal class ClusterWithInvalidPortsProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("that include a negative number", NegativePort::class.java)),
                arguments(named("that include a negative number for a sentinel", NegativePortSentinel::class.java)),
                arguments(named("that include 65536", Port65536::class.java)),
                arguments(named("that include 65536 for a sentinel", Port65536Sentinel::class.java))
            )
        }

        @EmbeddedRedisCluster(replicationGroups = [ReplicationGroup(ports = [-1, 1, 2])])
        internal class NegativePort : WithDummyTest()

        @EmbeddedRedisCluster(sentinels = [Sentinel(port = -1)])
        internal class NegativePortSentinel : WithDummyTest()

        @EmbeddedRedisCluster(replicationGroups = [ReplicationGroup(ports = [65534, 65535, 65536])])
        internal class Port65536 : WithDummyTest()

        @EmbeddedRedisCluster(sentinels = [Sentinel(port = 65536)])
        internal class Port65536Sentinel : WithDummyTest()
    }

    @EmbeddedRedisCluster(sentinels = [Sentinel(port = 1, monitoredGroups = ["unknown"])])
    internal class SentinelWithUnknownMonitoredGroup : WithDummyTest()

    @EmbeddedRedisCluster(customizer = [ClusterCustomizerWithoutNoArgsConstructor::class])
    internal class ClusterWithCustomizerWithoutNoArgsConstructor : WithDummyTest()

    internal class ClusterCustomizerWithoutNoArgsConstructor(val sth: String) : RedisClusterCustomizer {
        override fun customizeMainNode(builder: RedisServerBuilder, config: EmbeddedRedisCluster, group: String) {
            // no-op
        }

        override fun customizeReplicas(builder: List<RedisServerBuilder>, config: EmbeddedRedisCluster, group: String) {
            // no-op
        }

        override fun customizeSentinels(
            builder: RedisSentinelBuilder,
            config: EmbeddedRedisCluster,
            sentinelConfig: Sentinel
        ) {
            // no-op
        }

    }

    internal class ClusterWithDuplicatePortsProvider : ArgumentsProvider {

        override fun provideArguments(context: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(named("duplicate ports within a replication group", DupPortsSingleGroup::class.java)),
                arguments(named("duplicate ports across two replication groups", DupPortsTwoGroups::class.java)),
                arguments(
                    named(
                        "duplicate ports across a replication group and a sentinel",
                        DupPortsGroupAndSentinel::class.java
                    )
                ),
                arguments(named("duplicate ports across two sentinels", DupPortsTwoSentinels::class.java)),
            )
        }

        @EmbeddedRedisCluster(replicationGroups = [ReplicationGroup(ports = [1, 2, 2])])
        internal class DupPortsSingleGroup : WithDummyTest()

        @EmbeddedRedisCluster(replicationGroups = [ReplicationGroup(ports = [1, 2, 3]), ReplicationGroup(ports = [1, 2, 3])])
        internal class DupPortsTwoGroups : WithDummyTest()

        @EmbeddedRedisCluster(
            replicationGroups = [ReplicationGroup(ports = [1, 2, 3])],
            sentinels = [Sentinel(port = 1)]
        )
        internal class DupPortsGroupAndSentinel : WithDummyTest()

        @EmbeddedRedisCluster(
            sentinels = [Sentinel(port = 1), Sentinel(port = 1)]
        )
        internal class DupPortsTwoSentinels : WithDummyTest()
    }

    internal abstract class WithDummyTest {
        @Test
        fun dummy() {
        }
    }
}