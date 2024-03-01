package io.github.tobi.laa.spring.boot.embedded.redis.standalone

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import io.github.tobi.laa.spring.boot.embedded.redis.standalone.CustomCustomizerTest.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import redis.embedded.core.RedisServerBuilder

private const val TEST_PORT = 10000

@IntegrationTest
@EmbeddedRedisStandalone(
    customizer = [
        WillBeIgnored::class,
        SetsPortToTestPort::class,
        SetsBindToLoopbackIpv4::class,
        SetsProtectedMode::class]
)
@DisplayName("Using @EmbeddedRedisStandalone with several customizers")
internal open class CustomCustomizerTest {

    @Autowired
    private lateinit var given: RedisTests

    @Test
    @DisplayName("RedisProperties should have the customized values")
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
    fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
        given.randomTestdata()
            .whenRedis().isBeingWrittenTo()
            .then().redis().shouldContainTheTestdata()
    }

    @Test
    @DisplayName("Settings from customizer should have been applied to the embedded Redis server")
    fun settingsFromCustomizerShouldHaveBeenApplied() {
        given.nothing()
            .whenDoingNothing()
            .then().embeddedRedis()
            .shouldHaveConfig().thatContainsDirective("protected-mode", "yes")
    }

    class WillBeIgnored : RedisStandaloneCustomizer {
        override fun accept(builder: RedisServerBuilder, config: EmbeddedRedisStandalone) {
            builder.port(1)
            builder.bind("zombo.com")
        }
    }

    class SetsPortToTestPort : RedisStandaloneCustomizer {
        override fun accept(builder: RedisServerBuilder, config: EmbeddedRedisStandalone) {
            builder.port(TEST_PORT)
        }
    }

    class SetsBindToLoopbackIpv4 : RedisStandaloneCustomizer {
        override fun accept(builder: RedisServerBuilder, config: EmbeddedRedisStandalone) {
            builder.bind("127.0.0.1")
        }
    }

    class SetsProtectedMode : RedisStandaloneCustomizer {
        override fun accept(builder: RedisServerBuilder, config: EmbeddedRedisStandalone) {
            builder.setting("protected-mode yes")
        }
    }
}