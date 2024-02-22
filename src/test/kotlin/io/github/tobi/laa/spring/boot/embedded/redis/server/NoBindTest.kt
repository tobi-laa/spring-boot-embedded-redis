package io.github.tobi.laa.spring.boot.embedded.redis.server

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
@EmbeddedRedisServer(bind = "")
@DisplayName("Using @EmbeddedRedisServer with empty bind")
internal class NoBindTest {

    @Autowired
    private lateinit var given: RedisTests

    @Test
    @DisplayName("RedisProperties should have default bind from embedded-redis library")
    fun redisPropertiesShouldHaveDefaultValues() {
        given.nothing()
            .whenDoingNothing()
            .then().redisProperties()
            .shouldBeStandalone().and()
            .shouldHaveHost("127.0.0.1")
    }

    @Test
    @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
    fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
        given.randomTestdata()
            .whenRedis().isBeingWrittenTo()
            .then().redis().shouldContainTheTestdata()
    }
}