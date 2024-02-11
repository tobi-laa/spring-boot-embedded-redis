package io.github.tobi.laa.spring.boot.embedded.redis.server

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import io.github.tobi.laa.spring.boot.embedded.redis.server.CustomizerTest.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import redis.embedded.core.RedisServerBuilder

private const val TEST_PORT = 11111

@IntegrationTest
@EmbeddedRedisServer(customizer = [WillBeIgnored::class, SetsPort::class, SetsProtectedMode::class])
@DisplayName("Using @EmbeddedRedisServer with several customizers")
internal open class CustomizerTest {

    @Autowired
    private lateinit var given: RedisTests

    @Test
    @Order(1)
    fun redisPropertiesShouldHaveCustomizedValues() {
        given.nothing()
            .whenDoingNothing()
            .then().redisProperties()
            .shouldBeStandalone().and()
            .shouldHaveHost("localhost").and()
            .shouldHavePort(TEST_PORT)
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
    fun settingsFromCustomizerShouldHaveBeenApplied() {
        given.nothing()
            .whenDoingNothing()
            .then().embeddedRedis()
            .shouldHaveConfigFile().thatContainsSetting("protected-mode yes")
    }

    class WillBeIgnored : RedisServerCustomizer {
        override fun accept(builder: RedisServerBuilder, config: EmbeddedRedisServer) {
            builder.port(1)
        }
    }

    class SetsPort : RedisServerCustomizer {
        override fun accept(builder: RedisServerBuilder, config: EmbeddedRedisServer) {
            builder.port(TEST_PORT)
        }
    }

    class SetsProtectedMode : RedisServerCustomizer {
        override fun accept(builder: RedisServerBuilder, config: EmbeddedRedisServer) {
            builder.setting("protected-mode yes")
        }
    }
}