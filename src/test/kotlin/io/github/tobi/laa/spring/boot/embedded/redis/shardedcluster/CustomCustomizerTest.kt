package io.github.tobi.laa.spring.boot.embedded.redis.shardedcluster

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import io.github.tobi.laa.spring.boot.embedded.redis.shardedcluster.CustomCustomizerTest.SetsPortOfMainNodeForCappedHeron
import io.github.tobi.laa.spring.boot.embedded.redis.shardedcluster.CustomCustomizerTest.SetsProtectedModeForReplicasOfBareThroatedTigerHeron
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import redis.embedded.core.RedisServerBuilder

private const val CAPPED_HERON_MAIN_PORT = 10000

@IntegrationTest
@EmbeddedRedisShardedCluster(
    shards = [
        EmbeddedRedisShardedCluster.Shard(name = "Capped Heron"),
        EmbeddedRedisShardedCluster.Shard(name = "Zigzag Heron"),
        EmbeddedRedisShardedCluster.Shard(name = "Bare-throated Tiger Heron")],
    customizer = [
        SetsPortOfMainNodeForCappedHeron::class,
        SetsProtectedModeForReplicasOfBareThroatedTigerHeron::class]
)
@DisplayName("Using @EmbeddedRedisShardedCluster with several customizers")
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
            .shouldHaveNode("127.0.0.1", CAPPED_HERON_MAIN_PORT)
    }

    @Test
    @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
    fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
        given.randomTestdata()
            .whenRedis().isBeingWrittenTo()
            .then().redis().shouldContainTheTestdata()
    }

    @Test
    @DisplayName("Settings from customizer should have been applied to the embedded Redis server")
    fun settingsFromCustomizerShouldHaveBeenApplied() {
        given.nothing()
            .whenDoingNothing()
            .then().embeddedRedis()
            .shouldHaveConfig().thatContainsDirective("protected-mode", "yes")
    }

    class SetsPortOfMainNodeForCappedHeron : RedisShardCustomizer {
        override fun customizeMainNode(
            builder: RedisServerBuilder,
            config: EmbeddedRedisShardedCluster,
            shard: String
        ) {
            if (shard == "Capped Heron") {
                builder.port(CAPPED_HERON_MAIN_PORT)
            }
        }

        override fun customizeReplicas(
            builder: List<RedisServerBuilder>,
            config: EmbeddedRedisShardedCluster,
            shard: String
        ) {
            // no-op
        }
    }

    class SetsProtectedModeForReplicasOfBareThroatedTigerHeron : RedisShardCustomizer {
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
            if (shard == "Bare-throated Tiger Heron") {
                builder.forEach { it.setting("protected-mode yes") }
            }
        }
    }
}