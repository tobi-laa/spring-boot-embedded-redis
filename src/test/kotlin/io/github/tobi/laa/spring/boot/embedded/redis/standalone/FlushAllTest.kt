package io.github.tobi.laa.spring.boot.embedded.redis.standalone

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisFlushAll
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired

@IntegrationTest
@EmbeddedRedisStandalone
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestClassOrder(ClassOrderer.OrderAnnotation::class)
@DisplayName("Test @RedisFlushAll annotation for standalone Redis server")
internal class FlushAllTest {

    @Autowired
    private lateinit var given: RedisTests

    @Nested
    @DisplayName("Not specifying @FlushAll should default to flushing all data after each test")
    @Order(1)
    inner class NotAnnotated {

        @Test
        @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
        @Order(1)
        fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
            given.randomTestdata()
                .whenRedis().isBeingWrittenTo()
                .then().redis().shouldContainTheTestdata()
        }

        @Test
        @DisplayName("Redis should have been flushed after the first test")
        @Order(2)
        fun redisShouldHaveBeenFlushed() {
            given.nothing()
                .whenDoingNothing()
                .then().redis().shouldNotContainAnyTestdata()
        }
    }

    @Nested
    @RedisFlushAll
    @DisplayName("Specifying @RedisFlushAll with default settings should flush all data after each test")
    @Order(2)
    inner class Default {

        @Test
        @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
        @Order(1)
        fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
            given.randomTestdata()
                .whenRedis().isBeingWrittenTo()
                .then().redis().shouldContainTheTestdata()
        }

        @Test
        @DisplayName("Redis should have been flushed after the first test")
        @Order(2)
        fun redisShouldHaveBeenFlushed() {
            given.nothing()
                .whenDoingNothing()
                .then().redis().shouldNotContainAnyTestdata()
        }
    }

    @Nested
    @RedisFlushAll(RedisFlushAll.Mode.AFTER_CLASS)
    @DisplayName("Specifying @RedisFlushAll with mode AFTER_CLASS should flush all data after the test class")
    @Order(3)
    inner class AfterClass {

        @Test
        @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
        @Order(1)
        fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
            given.randomTestdata()
                .whenRedis().isBeingWrittenTo()
                .then().redis().shouldContainTheTestdata()
        }

        @Test
        @DisplayName("Redis should not have been flushed after the first test")
        @Order(2)
        fun redisShouldNotHaveBeenFlushed() {
            given.nothing()
                .whenDoingNothing()
                .then().redis().shouldContainTheTestdata()
        }
    }

    @Nested
    @DisplayName("The assertion for the previously executed AfterClass")
    @Order(4)
    inner class AfterClassAssertion {

        @Test
        @DisplayName("Redis should have been flushed after the test class 'AfterClass'")
        fun redisShouldHaveBeenFlushed() {
            given.nothing()
                .whenDoingNothing()
                .then().redis().shouldNotContainAnyTestdata()
        }
    }

    @Nested
    @RedisFlushAll(RedisFlushAll.Mode.NEVER)
    @DisplayName("Specifying @RedisFlushAll with mode NEVER should not flush any data")
    @Order(5)
    inner class Never {

        @Test
        @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
        @Order(1)
        fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
            given.randomTestdata()
                .whenRedis().isBeingWrittenTo()
                .then().redis().shouldContainTheTestdata()
        }

        @Test
        @DisplayName("Redis should not have been flushed after the first test")
        @Order(2)
        fun redisShouldNotHaveBeenFlushed() {
            given.nothing()
                .whenDoingNothing()
                .then().redis().shouldContainTheTestdata()
        }
    }

    @Nested
    @DisplayName("The assertion for the previously executed Never")
    @Order(6)
    inner class NeverAssertion {

        @Test
        @DisplayName("Redis should not have been flushed after the test class 'Never'")
        fun redisShouldNotHaveBeenFlushed() {
            given.nothing()
                .whenDoingNothing()
                .then().redis().shouldContainTheTestdata()
        }
    }
}