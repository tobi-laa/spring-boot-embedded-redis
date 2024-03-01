package io.github.tobi.laa.spring.boot.embedded.redis.cluster

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import io.github.tobi.laa.spring.boot.embedded.redis.cluster.EmbeddedRedisCluster.Shard
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Duration.ofSeconds

@IntegrationTest
@EmbeddedRedisCluster(
    shards = [
        Shard(replicas = 1),
        Shard(replicas = 2),
        Shard(replicas = 3)],
    ports = [6379, 0, 7001, 7002, 7003, 7004, 7005, 11111, 22222]
)
@DisplayName("Using @EmbeddedRedisCluster with custom settings")
internal class CustomSettingsTest {

    @Autowired
    private lateinit var given: RedisTests

    @Test
    @DisplayName("RedisProperties should have the customized values")
    fun redisPropertiesShouldHaveCustomizedValues() {
        given.nothing()
            .whenDoingNothing()
            .then().redisProperties()
            .shouldBeCluster()
            .shouldHaveNode("127.0.0.1", 6379)
            .shouldHaveNode("127.0.0.1", 6380)
            .shouldHaveNode("127.0.0.1", 7001)
            .shouldHaveNode("127.0.0.1", 7002)
            .shouldHaveNode("127.0.0.1", 7003)
            .shouldHaveNode("127.0.0.1", 7004)
            .shouldHaveNode("127.0.0.1", 7005)
            .shouldHaveNode("127.0.0.1", 11111)
            .shouldHaveNode("127.0.0.1", 22222)
            .andAlso().embeddedRedis()
            .shouldHaveInitializationTimeout(ofSeconds(20))
    }

    @Test
    @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
    fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
        given.randomTestdata()
            .whenRedis().isBeingWrittenTo()
            .then().redis().shouldContainTheTestdata()
    }
}