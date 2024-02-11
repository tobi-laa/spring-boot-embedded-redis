package io.github.tobi.laa.spring.boot.embedded.redis

import org.springframework.context.ApplicationContext
import redis.embedded.Redis
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds the Redis instances per Spring application context.
 */
internal object RedisStore {

    private val internalStore = ConcurrentHashMap<ApplicationContext, Redis>()

    internal fun computeIfAbsent(context: ApplicationContext, redis: () -> Redis): Redis {
        return internalStore.computeIfAbsent(context) { _ -> redis.invoke() }
    }

    internal fun get(context: ApplicationContext): Redis? {
        return internalStore[context]
    }
}