package io.github.tobi.laa.spring.boot.embedded.redis.server

import io.github.tobi.laa.spring.boot.embedded.redis.RedisStore
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertSame
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.annotation.DirtiesContext
import redis.embedded.Redis

@DisplayName("Re-using Spring context should not start a new Redis server")
@TestClassOrder(ClassOrderer.OrderAnnotation::class)
internal class SingletonTest {

    @Nested
    @SpringBootTest
    @EmbeddedRedisServer
    @DisplayName("Using @EmbeddedRedisServer with default settings for a class")
    @Order(1)
    internal inner class First {

        @Autowired
        private lateinit var context: ApplicationContext

        @Autowired
        private lateinit var given: RedisTests

        @Test
        @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
        fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
            given.randomTestdata()
                .whenRedis().isBeingWrittenTo()
                .then().redis().shouldContainTheTestdata()
        }

        @AfterEach
        fun setRedis() {
            firstRedis = RedisStore.server(context)!!
        }
    }

    @Nested
    @SpringBootTest
    @EmbeddedRedisServer
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
    @DisplayName("Using @EmbeddedRedisServer with default settings for another class")
    @Order(2)
    internal inner class Second {

        @Autowired
        private lateinit var context: ApplicationContext

        @Autowired
        private lateinit var given: RedisTests

        @Test
        @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
        fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
            given.randomTestdata()
                .whenRedis().isBeingWrittenTo()
                .then().redis().shouldContainTheTestdata()
        }

        @AfterEach
        fun setRedis() {
            secondRedis = RedisStore.server(context)!!
        }
    }

    @Nested
    @DisplayName("Embedded Redis server should be the same for both classes")
    @Order(3)
    internal inner class AssertSameRedis {

        @Test
        @DisplayName("The Redis server should be the same for both classes")
        fun redisServersShouldBeTheSame() {
            assertSame(firstRedis, secondRedis)
        }
    }

    companion object {

        private lateinit var firstRedis: Redis

        private lateinit var secondRedis: Redis
    }
}