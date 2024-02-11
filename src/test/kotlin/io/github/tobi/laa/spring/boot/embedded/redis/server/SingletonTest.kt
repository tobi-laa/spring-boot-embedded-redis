package io.github.tobi.laa.spring.boot.embedded.redis.server

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import org.junit.jupiter.api.DisplayName

@IntegrationTest
@EmbeddedRedisServer
@DisplayName("Re-using Spring context from DefaultSettingsTest should not start a new Redis server")
internal class SingletonTest : DefaultSettingsTest() {
    // no content
}