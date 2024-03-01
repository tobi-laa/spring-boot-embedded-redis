package io.github.tobi.laa.spring.boot.embedded.redis.standalone

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

private const val TEST_PORT = 10001
private const val TEST_BIND = "::1"

@IntegrationTest
@EmbeddedRedisStandalone(
    port = TEST_PORT,
    bind = TEST_BIND,
    settings = [
        "appendonly no",
        "protected-mode yes",
        "appendfsync everysec"]
)
@DisplayName("Using @EmbeddedRedisStandalone with custom settings")
internal class CustomSettingsTest {

    @Autowired
    private lateinit var given: RedisTests

    @Test
    @DisplayName("RedisProperties should have the customized values")
    fun redisPropertiesShouldHaveCustomizedValues() {
        given.nothing()
            .whenDoingNothing()
            .then().redisProperties()
            .shouldBeStandalone().and()
            .shouldHaveHost(TEST_BIND).and()
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
    @DisplayName("Settings from @EmbeddedRedisStandalone should have been applied to the embedded Redis server")
    fun configFileShouldHaveBeenApplied() {
        given.nothing()
            .whenDoingNothing()
            .then().embeddedRedis()
            .shouldHaveConfig().thatContainsDirective("appendonly", "no")
            .and().thatContainsDirective("protected-mode", "yes")
            .and().thatContainsDirective("appendfsync", "everysec")
    }
}