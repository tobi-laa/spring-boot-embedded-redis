package io.github.tobi.laa.spring.boot.embedded.redis.server

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
@EmbeddedRedisServer(
    port = 12345,
    configFile = "src/test/resources/redis/conf/redis.conf"
)
@DisplayName("Using @EmbeddedRedisServer with config file")
internal class RedisConfFileTest {

    @Autowired
    private lateinit var given: RedisTests

    @Test
    @Order(1)
    fun redisPropertiesShouldHaveConfiguredValues() {
        given.nothing()
            .whenDoingNothing()
            .then().redisProperties()
            .shouldBeStandalone().and()
            .shouldHaveHost("localhost").and()
            .shouldHavePort(12345)
    }

    @Test
    @Order(2)
    fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
        given.randomTestdata()
            .whenRedis().isBeingWrittenTo()
            .then().redis().shouldContainTheTestdata()
    }

    @Test
    @Order(3)
    fun redisShouldHaveBeenFlushed() {
        given.nothing()
            .whenDoingNothing()
            .then().redis().shouldNotContainAnyTestdata()
    }

    @Test
    @Order(4)
    fun configFileShouldHaveBeenApplied() {
        given.nothing()
            .whenDoingNothing()
            .then().embeddedRedis()
            .shouldHaveConfigFile().thatContainsSetting("appendonly no")
            .and().thatContainsSetting("protected-mode yes")
            .and().thatContainsSetting("appendfsync everysec")
    }
}