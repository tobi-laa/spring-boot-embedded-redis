package io.github.tobi.laa.spring.boot.embedded.redis.highavailability

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisStore
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.*
import org.springframework.context.ApplicationContext
import redis.embedded.Redis
import redis.embedded.RedisCluster

@IntegrationTest
@EmbeddedRedisHighAvailability
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@DisplayName("Parameter resolver test for @EmbeddedRedisHighAvailability")
internal class ParamResolverTest {

    var paramsFromBeforeEach = listOf<Redis>()
    var paramsFromAfterEach = listOf<Redis>()

    @Test
    @Order(1)
    @DisplayName("Should resolve @Test parameters correctly")
    fun shouldResolveTestParamsCorrectly(redis: Redis, redisCluster: RedisCluster) {
        thenParamIsCorrectlyResolved(redis)
        thenParamIsCorrectlyResolved(redisCluster)
    }

    @BeforeEach
    fun beforeEach(redis: Redis, redisCluster: RedisCluster) {
        paramsFromBeforeEach = listOf(redis, redisCluster)
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
    fun afterEach(redis: Redis, redisCluster: RedisCluster) {
        paramsFromAfterEach = listOf(redis, redisCluster)
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
            this.context = context
        }

        @JvmStatic
        @BeforeAll
        fun beforeAll(redis: Redis, redisCluster: RedisCluster) {
            paramsFromBeforeAll = listOf(redis, redisCluster)
        }

        @JvmStatic
        @AfterAll
        fun afterAll(redis: Redis, redisCluster: RedisCluster) {
            // no @Test methods will be executed after this method, so assertions are put here instead
            thenParamIsCorrectlyResolved(redis)
            thenParamIsCorrectlyResolved(redisCluster)
        }

        private fun thenParamIsCorrectlyResolved(param: Any?) {
            Assertions.assertThat(param).isNotNull.isEqualTo(RedisStore.server(context!!)!!)
        }
    }
}