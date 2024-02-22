package io.github.tobi.laa.spring.boot.embedded.redis.shardedcluster

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
@EmbeddedRedisShardedCluster
@DisplayName("Using @EmbeddedRedisShardedCluster with default settings")
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
            .shouldHaveNode("127.0.0.1", 6379)
            .shouldHaveNode("127.0.0.1", 6380)
            .shouldHaveNode("127.0.0.1", 6381)
    }

    @Test
    @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
    fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
        given.randomTestdata()
            .whenRedis().isBeingWrittenTo()
            .then().redis().shouldContainTheTestdata()
    }
}