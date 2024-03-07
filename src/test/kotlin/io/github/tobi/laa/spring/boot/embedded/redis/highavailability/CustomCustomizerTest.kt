package io.github.tobi.laa.spring.boot.embedded.redis.highavailability

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import io.github.tobi.laa.spring.boot.embedded.redis.highavailability.CustomCustomizerTest.*
import io.github.tobi.laa.spring.boot.embedded.redis.highavailability.EmbeddedRedisHighAvailability.Sentinel
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import redis.embedded.core.RedisSentinelBuilder
import redis.embedded.core.RedisServerBuilder

private const val CAPPED_HERON_MAIN_PORT = 10000
private const val SENTINEL_PORT = 30000

@IntegrationTest
@EmbeddedRedisHighAvailability(
    name = "Capped Heron",
    sentinels = [Sentinel()],
    customizer = [
        SetsPortOfMainNodeForCappedHeron::class,
        SetsPortOfSentinel::class,
        // this customizer will have no effect as it is not applied to "Capped Heron"
        SetsProtectedModeForReplicasOfBareThroatedTigerHeron::class]
)
@DisplayName("Using @EmbeddedRedisHighAvailability with several customizers")
internal class CustomCustomizerTest {

    @Autowired
    private lateinit var given: RedisTests

    @Test
    @DisplayName("RedisProperties should have the customized values")
    fun redisPropertiesShouldHaveCustomizedValues() {
        given.nothing()
            .whenDoingNothing()
            .then().redisProperties()
            .shouldBeSentinel()
            .and().shouldHaveNode("localhost", SENTINEL_PORT)
    }

    @Test
    @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
    fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
        given.randomTestdata()
            .whenRedis().isBeingWrittenTo()
            .then().redis().shouldContainTheTestdata()
    }

    @Test
    @DisplayName("Settings from customizer should have been applied to the embedded Redis in high availability mode")
    fun settingsFromCustomizerShouldHaveBeenApplied() {
        given.nothing()
            .whenDoingNothing()
            .then().embeddedRedis()
            .shouldHaveConfig().thatDoesNotContainDirective("protected-mode", "yes")
            .andAlso().embeddedRedis()
            .shouldHaveNodes()
            .thatHaveASizeOf(3)
            .withOne().thatRunsOn("127.0.0.1", CAPPED_HERON_MAIN_PORT)
    }

    internal class SetsPortOfMainNodeForCappedHeron : RedisHighAvailabilityCustomizer {
        override fun customizeMainNode(builder: RedisServerBuilder, config: EmbeddedRedisHighAvailability) {
            if (config.name == "Capped Heron") {
                builder.port(CAPPED_HERON_MAIN_PORT)
            }
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

    internal class SetsPortOfSentinel : RedisHighAvailabilityCustomizer {
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
            builder.port(SENTINEL_PORT)
        }
    }

    internal class SetsProtectedModeForReplicasOfBareThroatedTigerHeron : RedisHighAvailabilityCustomizer {
        override fun customizeMainNode(builder: RedisServerBuilder, config: EmbeddedRedisHighAvailability) {
            // no-op
        }

        override fun customizeReplicas(builder: List<RedisServerBuilder>, config: EmbeddedRedisHighAvailability) {
            if (config.name == "Bare-throated Tiger Heron") {
                builder.forEach { it.setting("protected-mode yes") }
            }
        }

        override fun customizeSentinels(
            builder: RedisSentinelBuilder,
            config: EmbeddedRedisHighAvailability,
            sentinelConfig: Sentinel
        ) {
            // no-op
        }
    }
}