package io.github.tobi.laa.spring.boot.embedded.redis.server

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired


@IntegrationTest
@EmbeddedRedisServer(
    configFile = "src/test/resources/server/redis.conf"
)
@DisplayName("Using @EmbeddedRedisServer with config file")
internal class RedisConfFileTest {

    @Autowired
    private lateinit var given: RedisTests

    @Test
    @DisplayName("RedisProperties should have the values as given in the redis.conf file")
    fun redisPropertiesShouldHaveConfiguredValues() {
        given.nothing()
            .whenDoingNothing()
            .then().redisProperties()
            .shouldBeStandalone().and()
            .shouldHaveHost("localhost")
    }

    @Test
    @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
    fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
        given.randomTestdata()
            .whenRedis().isBeingWrittenTo()
            .then().redis().shouldContainTheTestdata()
    }

    @Test
    @DisplayName("Settings from redis.conf should have been applied to the embedded Redis server")
    fun configFileShouldHaveBeenApplied() {
        given.nothing()
            .whenDoingNothing()
            .then().embeddedRedis()
            .shouldHaveConfig().thatContainsDirective("appendonly", "no")
            .and().thatContainsDirective("protected-mode", "yes")
            .and().thatContainsDirective("appendfsync", "everysec")
    }
}