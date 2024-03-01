package io.github.tobi.laa.spring.boot.embedded.redis.cluster

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import io.github.tobi.laa.spring.boot.embedded.redis.cluster.DefaultSettingsTest.NameCapturingCustomizer
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import redis.embedded.core.RedisSentinelBuilder
import redis.embedded.core.RedisServerBuilder

@IntegrationTest
@EmbeddedRedisCluster(customizer = [NameCapturingCustomizer::class])
@DisplayName("Using @EmbeddedRedisCluster with default settings")
internal class DefaultSettingsTest {

    @Autowired
    private lateinit var given: RedisTests

    @Test
    @DisplayName("RedisProperties should have the default values")
    fun redisPropertiesShouldHaveDefaultValues() {
        given.nothing()
            .whenDoingNothing()
            .then().redisProperties()
            .shouldBeSentinel()
            .shouldHaveNode("localhost", 26379)
    }

    @Test
    @DisplayName("Three nodes should have been started with default values")
    fun nodesShouldHaveDefaultValues() {
        given.nothing()
            .whenDoingNothing()
            .then().embeddedRedis()
            .shouldHaveNodes()
            .thatHaveASizeOf(3)
            .withOne().thatRunsOn("::1", 6379)
            .and().withOne().thatRunsOn("::1", 6380)
            .and().withOne().thatRunsOn("::1", 6381)
    }

    @Test
    @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
    fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
        given.randomTestdata()
            .whenRedis().isBeingWrittenTo()
            .then().redis().shouldContainTheTestdata()
    }

    companion object {
        lateinit var name: String
    }

    internal class NameCapturingCustomizer : RedisClusterCustomizer {

        override fun customizeMainNode(
            builder: RedisServerBuilder,
            config: EmbeddedRedisCluster
        ) {
            name = config.name
        }

        override fun customizeReplicas(
            builder: List<RedisServerBuilder>,
            config: EmbeddedRedisCluster
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