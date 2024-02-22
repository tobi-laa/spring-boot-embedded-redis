package io.github.tobi.laa.spring.boot.embedded.redis

import org.springframework.context.ApplicationContext
import redis.clients.jedis.UnifiedJedis
import redis.embedded.Redis
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds the Redis instances per Spring application context.
 */
internal object RedisStore {

    private val internalStore = ConcurrentHashMap<ApplicationContext, Pair<Redis, UnifiedJedis>>()

    internal fun computeIfAbsent(
        context: ApplicationContext,
        supplier: () -> Pair<Redis, UnifiedJedis>
    ) {
        internalStore.computeIfAbsent(context) { _ -> supplier.invoke() }
    }

    internal fun server(context: ApplicationContext): Redis? {
        return internalStore[context]?.first
    }

    internal fun client(context: ApplicationContext): UnifiedJedis? {
        return internalStore[context]?.second
    }
}