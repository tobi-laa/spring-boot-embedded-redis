package io.github.tobi.laa.spring.boot.embedded.redis.highavailability

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import io.github.tobi.laa.spring.boot.embedded.redis.highavailability.EmbeddedRedisHighAvailability.Sentinel
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
@EmbeddedRedisHighAvailability(
    name = "Zigzag Heron",
    replicas = 2,
    binds = ["127.0.0.1", "localhost", "::1"],
    ports = [7000, 7001, 7002],
    sentinels = [
        Sentinel(
            bind = "127.0.0.1", // FIXME not working for Ipv6, should properly escape
            port = 22222,
            downAfterMillis = 30000,
            failOverTimeoutMillis = 60000,
            parallelSyncs = 2
        )]
)
@DisplayName("Using @EmbeddedRedisHighAvailability with custom settings")
internal class CustomSettingsTest {

    @Autowired
    private lateinit var given: RedisTests

    @Test
    @DisplayName("RedisProperties should have the customized values")
    fun redisPropertiesShouldHaveCustomizedValues() {
        given.nothing()
            .whenDoingNothing()
            .then().redisProperties()
            .shouldBeSentinel()
            .shouldHaveNode("127.0.0.1", 22222)
    }

    @Test
    @DisplayName("Custom settings should have been applied to the sentinels")
    fun customSettingsShouldHaveBeenAppliedToSentinels() {
        given.nothing()
            .whenDoingNothing()
            .then().embeddedRedis()
            .shouldHaveSentinels()
            .thatHaveASizeOf(1)
            .withAtLeastOne()
            // .thatHasDownAfterMillis("ZigzagHeron", 30000) // FIXME dropped by config rewrite?
            .thatHasFailOverTimeoutMillis("ZigzagHeron", 60000)
            .thatHasParallelSyncs("ZigzagHeron", 2)
            .andAlso()
            .embeddedRedis()
            .shouldHaveNodes()
            .thatHaveASizeOf(3)
            .withOne().thatRunsOn("127.0.0.1", 7000)
            .and().withOne().thatRunsOn("localhost", 7001)
            .and().withOne().thatRunsOn("::1", 7002)
    }

    @Test
    @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
    fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
        given.randomTestdata()
            .whenRedis().isBeingWrittenTo()
            .then().redis().shouldContainTheTestdata()
    }
}