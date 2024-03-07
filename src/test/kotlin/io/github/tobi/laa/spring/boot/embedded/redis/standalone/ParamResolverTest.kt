package io.github.tobi.laa.spring.boot.embedded.redis.standalone

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisStore
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import org.springframework.context.ApplicationContext
import redis.embedded.Redis
import redis.embedded.RedisInstance
import redis.embedded.RedisServer

@IntegrationTest
@EmbeddedRedisStandalone
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@DisplayName("Parameter resolver test for @EmbeddedRedisStandalone")
internal class ParamResolverTest {

    var paramsFromBeforeEach = listOf<Redis>()
    var paramsFromAfterEach = listOf<Redis>()

    @Test
    @Order(1)
    @DisplayName("Should resolve @Test parameters correctly")
    fun shouldResolveTestParamsCorrectly(redis: Redis, redisServer: RedisServer, redisInstance: RedisInstance) {
        thenParamIsCorrectlyResolved(redis)
        thenParamIsCorrectlyResolved(redisServer)
        thenParamIsCorrectlyResolved(redisInstance)
    }

    @BeforeEach
    fun beforeEach(redis: Redis, redisServer: RedisServer, redisInstance: RedisInstance) {
        paramsFromBeforeEach = listOf(redis, redisServer, redisInstance)
    }

    @Test
    @Order(2)
    @DisplayName("Should resolve @BeforeEach params correctly")
    fun shouldResolveBeforeEachParamsCorrectly() {
        paramsFromBeforeEach.forEach { thenParamIsCorrectlyResolved(it) }
    }

    @Test
    @Order(3)
    @DisplayName("Should resolve @BeforeAll params correctly")
    fun shouldResolveBeforeAllParamsCorrectly() {
        paramsFromBeforeAll.forEach { thenParamIsCorrectlyResolved(it) }
    }

    @AfterEach
    fun afterEach(redis: Redis, redisServer: RedisServer, redisInstance: RedisInstance) {
        paramsFromAfterEach = listOf(redis, redisServer, redisInstance)
    }

    @Test
    @Order(4)
    @DisplayName("Should resolve @AfterEach params correctly")
    fun shouldResolveAfterEachParamsCorrectly() {
        paramsFromAfterEach.forEach { thenParamIsCorrectlyResolved(it) }
    }

    private companion object {

        var paramsFromBeforeAll = listOf<Redis>()

        var context: ApplicationContext? = null

        @JvmStatic
        @BeforeAll
        fun injectContext(context: ApplicationContext) {
            Companion.context = context
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll(redis: Redis, redisServer: RedisServer, redisInstance: RedisInstance) {
            paramsFromBeforeAll = listOf(redis, redisServer, redisInstance)
        }

        @JvmStatic
        @AfterAll
        fun afterAll(redis: Redis, redisServer: RedisServer, redisInstance: RedisInstance) {
            // no @Test methods will be executed after this method, so assertions are put here instead
            thenParamIsCorrectlyResolved(redis)
            thenParamIsCorrectlyResolved(redisServer)
            thenParamIsCorrectlyResolved(redisInstance)
        }

        private fun thenParamIsCorrectlyResolved(param: Any?) {
            Assertions.assertThat(param).isNotNull.isEqualTo(RedisStore.server(context!!)!!)
        }
    }
}