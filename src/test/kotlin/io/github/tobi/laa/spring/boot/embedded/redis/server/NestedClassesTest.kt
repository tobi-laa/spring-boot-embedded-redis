package io.github.tobi.laa.spring.boot.embedded.redis.server

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@EmbeddedRedisServer
@DisplayName("Using @EmbeddedRedisServer on a top level class with nested classes should work fine")
class NestedClassesTest {

    @Nested
    @IntegrationTest
    @DisplayName("Nested class should work with embedded Redis server from top level class")
    inner class InnerClass {

        @Autowired
        private lateinit var given: RedisTests

        @Test
        @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
        fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
            given.randomTestdata()
                .whenRedis().isBeingWrittenTo()
                .then().redis().shouldContainTheTestdata()
        }
    }
}