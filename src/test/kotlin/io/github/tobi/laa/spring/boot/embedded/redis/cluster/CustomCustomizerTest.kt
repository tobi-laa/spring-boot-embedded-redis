package io.github.tobi.laa.spring.boot.embedded.redis.cluster

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import io.github.tobi.laa.spring.boot.embedded.redis.cluster.CustomCustomizerTest.*
import io.github.tobi.laa.spring.boot.embedded.redis.cluster.EmbeddedRedisCluster.ReplicationGroup
import io.github.tobi.laa.spring.boot.embedded.redis.cluster.EmbeddedRedisCluster.Sentinel
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import redis.embedded.core.RedisSentinelBuilder
import redis.embedded.core.RedisServerBuilder

private const val CAPPED_HERON_MAIN_PORT = 10000
private const val ZIGZAG_HERON_SENTINEL_PORT = 30000

@IntegrationTest
@EmbeddedRedisCluster(
    replicationGroups = [
        ReplicationGroup(name = "Capped Heron"),
        ReplicationGroup(name = "Zigzag Heron"),
        ReplicationGroup(name = "Bare-throated Tiger Heron")],
    sentinels = [Sentinel(monitoredGroups = ["Zigzag Heron"])],
    customizer = [
        SetsPortOfMainNodeForCappedHeron::class,
        SetsPortOfSentinelMonitoringZigzagHeron::class,
        SetsProtectedModeForReplicasOfBareThroatedTigerHeron::class]
)
@DisplayName("Using @EmbeddedRedisCluster with several customizers")
internal open class CustomCustomizerTest {

    @Autowired
    private lateinit var given: RedisTests

    @Test
    @DisplayName("RedisProperties should have the customized values")
    fun redisPropertiesShouldHaveCustomizedValues() {
        given.nothing()
            .whenDoingNothing()
            .then().redisProperties()
            .shouldBeCluster()
            .and().shouldHaveNode("127.0.0.1", CAPPED_HERON_MAIN_PORT)
    }

    @Test
    @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
    fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
        given.randomTestdata()
            .whenRedis().isBeingWrittenTo()
            .then().redis().shouldContainTheTestdata()
    }

    @Test
    @DisplayName("Settings from customizer should have been applied to the embedded Redis cluster")
    fun settingsFromCustomizerShouldHaveBeenApplied() {
        given.nothing()
            .whenDoingNothing()
            .then().embeddedRedis()
            .shouldHaveConfig().thatContainsDirective("protected-mode", "yes")
            .andAlso().embeddedRedis()
            .shouldHaveSentinels()
            .thatHaveASizeOf(1)
            .withOne().thatRunsOn("localhost", ZIGZAG_HERON_SENTINEL_PORT)
    }

    internal class SetsPortOfMainNodeForCappedHeron : RedisClusterCustomizer {
        override fun customizeMainNode(builder: RedisServerBuilder, config: EmbeddedRedisCluster, group: String) {
            if (group == "Capped Heron") {
                builder.port(CAPPED_HERON_MAIN_PORT)
            }
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

    internal class SetsPortOfSentinelMonitoringZigzagHeron : RedisClusterCustomizer {
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
            if (sentinelConfig.monitoredGroups.contains("Zigzag Heron")) {
                builder.port(ZIGZAG_HERON_SENTINEL_PORT)
            }
        }
    }

    internal class SetsProtectedModeForReplicasOfBareThroatedTigerHeron : RedisClusterCustomizer {
        override fun customizeMainNode(builder: RedisServerBuilder, config: EmbeddedRedisCluster, group: String) {
            // no-op
        }

        override fun customizeReplicas(builder: List<RedisServerBuilder>, config: EmbeddedRedisCluster, group: String) {
            if (group == "Bare-throated Tiger Heron") {
                builder.forEach { it.setting("protected-mode yes") }
            }
        }

        override fun customizeSentinels(
            builder: RedisSentinelBuilder,
            config: EmbeddedRedisCluster,
            sentinelConfig: Sentinel
        ) {
            // no-op
        }
    }
}