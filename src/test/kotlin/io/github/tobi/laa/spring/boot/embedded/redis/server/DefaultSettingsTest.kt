package io.github.tobi.laa.spring.boot.embedded.redis.server

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
@EmbeddedRedisServer
@DisplayName("Using @EmbeddedRedisServer with default settings")
internal open class DefaultSettingsTest {

    @Autowired
    private lateinit var given: RedisTests

    @Test
    @Order(1)
    fun redisPropertiesShouldHaveDefaultValues() {
        given.nothing()
            .whenDoingNothing()
            .then().redisProperties()
            .shouldBeStandalone().and()
            .shouldHaveHost("localhost").and()
            .shouldHavePort(6379)
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
}