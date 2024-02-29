package io.github.tobi.laa.spring.boot.embedded.redis.cluster

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import io.github.tobi.laa.spring.boot.embedded.redis.cluster.DefaultSettingsTest.GroupNameCapturingCustomizer
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import redis.embedded.core.RedisSentinelBuilder
import redis.embedded.core.RedisServerBuilder

@IntegrationTest
@EmbeddedRedisCluster(customizer = [GroupNameCapturingCustomizer::class])
@DisplayName("Using @EmbeddedRedisCluster with default settings")
internal open class DefaultSettingsTest {

    @Autowired
    private lateinit var given: RedisTests

    @Test
    @DisplayName("RedisProperties should have the default values")
    fun redisPropertiesShouldHaveDefaultValues() {
        given.nothing()
            .whenDoingNothing()
            .then().redisProperties()
            .shouldBeCluster()
            .shouldHaveNode("localhost", 6379)
            .shouldHaveNode("localhost", 6380)
            .shouldHaveNode("localhost", 6381)
    }

    @Test
    @DisplayName("A sentinel monitoring the single replication group should have been started with default values")
    fun sentinelMonitoringSingleReplicationGroupShouldHaveDefaultValues() {
        given.nothing()
            .whenDoingNothing()
            .then().embeddedRedis()
            .shouldHaveSentinels()
            .thatHaveASizeOf(1)
            .withAtLeastOne().thatMonitors(group, "localhost", 6379)
    }

    @Test
    @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
    fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
        given.randomTestdata()
            .whenRedis().isBeingWrittenTo()
            .then().redis().shouldContainTheTestdata()
    }

    companion object {
        lateinit var group: String
    }

    internal class GroupNameCapturingCustomizer : RedisClusterCustomizer {

        override fun customizeMainNode(
            builder: RedisServerBuilder,
            config: EmbeddedRedisCluster,
            group: String
        ) {
            DefaultSettingsTest.group = group
        }

        override fun customizeReplicas(
            builder: List<RedisServerBuilder>,
            config: EmbeddedRedisCluster,
            group: String
        ) {
            // no-op
        }

        override fun customizeSentinels(
            builder: RedisSentinelBuilder,
            config: EmbeddedRedisCluster,
            sentinelConfig: EmbeddedRedisCluster.Sentinel
        ) {
            // no-op
        }
    }
}