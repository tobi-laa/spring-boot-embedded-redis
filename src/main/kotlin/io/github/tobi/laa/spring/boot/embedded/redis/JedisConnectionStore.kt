package io.github.tobi.laa.spring.boot.embedded.redis

import org.springframework.context.ApplicationContext
import redis.clients.jedis.JedisPooled
import redis.embedded.Redis
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds a Jedis connection pool for each embedded Redis instance per Spring application context.
 */
internal object JedisConnectionStore {

    private val internalStore = ConcurrentHashMap<ApplicationContext, JedisPooled>()

    internal fun getOrCreate(context: ApplicationContext): JedisPooled {
        return internalStore.getOrPut(context) {
            createConnectionPool(
                RedisStore.get(context)
                    ?: throw IllegalStateException("No embedded redis server found for application context: $context")
            )
        }
    }

    private fun createConnectionPool(redis: Redis): JedisPooled {
        return JedisPooled("localhost", redis.ports().first())
    }
}