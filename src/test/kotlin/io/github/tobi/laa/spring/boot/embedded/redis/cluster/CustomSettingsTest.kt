package io.github.tobi.laa.spring.boot.embedded.redis.cluster

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import io.github.tobi.laa.spring.boot.embedded.redis.cluster.EmbeddedRedisCluster.ReplicationGroup
import io.github.tobi.laa.spring.boot.embedded.redis.cluster.EmbeddedRedisCluster.Sentinel
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
@EmbeddedRedisCluster(
    replicationGroups = [
        ReplicationGroup(name = "Capped Heron", replicas = 1, binds = ["127.0.0.1", "::1"], ports = [6380, 6381]),
        ReplicationGroup(name = "Zigzag Heron", replicas = 2, ports = [7000, 7001, 7002]),
    ],
    sentinels = [
        Sentinel(
            bind = "::1",
            port = 22222,
            downAfterMillis = 30000,
            failOverTimeoutMillis = 60000,
            parallelSyncs = 2
        )]
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
            .shouldHaveNode("127.0.0.1", 6380)
            .shouldHaveNode("::1", 7000)
            .shouldHaveNode("localhost", 7001)
            .shouldHaveNode("localhost", 7002)
            .shouldHaveNode("localhost", 7003)
    }

    @Test
    @DisplayName("Custom settings should have been applied to the sentinels")
    fun customSettingsShouldHaveBeenAppliedToSentinels() {
        given.nothing()
            .whenDoingNothing()
            .then().embeddedRedis()
            .shouldHaveSentinels()
            .thatHaveASizeOf(1)
            .withOne().thatRunsOn("::1", 22222)
            .and().withAtLeastOne()
            .thatHasDownAfterMillis("Capped Heron", 30000)
            .thatHasFailOverTimeoutMillis("Capped Heron", 60000)
            .thatHasParallelSyncs("Capped Heron", 2)
    }

    @Test
    @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
    fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
        given.randomTestdata()
            .whenRedis().isBeingWrittenTo()
            .then().redis().shouldContainTheTestdata()
    }
}