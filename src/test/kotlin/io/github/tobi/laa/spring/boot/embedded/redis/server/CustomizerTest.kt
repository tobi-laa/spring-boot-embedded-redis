package io.github.tobi.laa.spring.boot.embedded.redis.server

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import io.github.tobi.laa.spring.boot.embedded.redis.server.CustomizerTest.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import redis.embedded.core.RedisServerBuilder

private const val TEST_PORT = 10000

@IntegrationTest
@EmbeddedRedisServer(
    customizer = [
        WillBeIgnored::class,
        SetsPortToTestPort::class,
        SetsBindToLoopbackIpv4::class,
        SetsProtectedMode::class]
)
@DisplayName("Using @EmbeddedRedisServer with several customizers")
internal open class CustomizerTest {

    @Autowired
    private lateinit var given: RedisTests

    @Test
    @DisplayName("RedisProperties should have the customized values")
    @Order(1)
    fun redisPropertiesShouldHaveCustomizedValues() {
        given.nothing()
            .whenDoingNothing()
            .then().redisProperties()
            .shouldBeStandalone().and()
            .shouldHaveHost("127.0.0.1").and()
            .shouldHavePort(TEST_PORT)
    }

    @Test
    @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
    @Order(2)
    fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
        given.randomTestdata()
            .whenRedis().isBeingWrittenTo()
            .then().redis().shouldContainTheTestdata()
    }

    @Test
    @DisplayName("Redis should have been flushed after the first test")
    @Order(3)
    fun redisShouldHaveBeenFlushed() {
        given.nothing()
            .whenDoingNothing()
            .then().redis().shouldNotContainAnyTestdata()
    }

    @Test
    @DisplayName("Settings from customizer should have been applied to the embedded Redis server")
    @Order(4)
    fun settingsFromCustomizerShouldHaveBeenApplied() {
        given.nothing()
            .whenDoingNothing()
            .then().embeddedRedis()
            .shouldHaveConfig().thatContainsDirective("protected-mode", "yes")
    }

    class WillBeIgnored : RedisServerCustomizer {
        override fun accept(builder: RedisServerBuilder, config: EmbeddedRedisServer) {
            builder.port(1)
            builder.bind("zombo.com")
        }
    }

    class SetsPortToTestPort : RedisServerCustomizer {
        override fun accept(builder: RedisServerBuilder, config: EmbeddedRedisServer) {
            builder.port(TEST_PORT)
        }
    }

    class SetsBindToLoopbackIpv4 : RedisServerCustomizer {
        override fun accept(builder: RedisServerBuilder, config: EmbeddedRedisServer) {
            builder.bind("127.0.0.1")
        }
    }

    class SetsProtectedMode : RedisServerCustomizer {
        override fun accept(builder: RedisServerBuilder, config: EmbeddedRedisServer) {
            builder.setting("protected-mode yes")
        }
    }
}