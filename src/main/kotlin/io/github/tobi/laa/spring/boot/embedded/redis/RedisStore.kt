package io.github.tobi.laa.spring.boot.embedded.redis

import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConf
import org.springframework.context.ApplicationContext
import redis.clients.jedis.JedisPooled
import redis.embedded.Redis
import java.util.concurrent.ConcurrentHashMap

/**
 * Holds the Redis instances per Spring application context.
 */
internal object RedisStore {

    private val internalStore = ConcurrentHashMap<ApplicationContext, Triple<Redis, RedisConf, JedisPooled>>()

    internal fun computeIfAbsent(
        context: ApplicationContext,
        supplier: () -> Triple<Redis, RedisConf, JedisPooled>
    ) {
        internalStore.computeIfAbsent(context) { _ -> supplier.invoke() }
    }

    internal fun server(context: ApplicationContext): Redis? {
        return internalStore[context]?.first
    }

    internal fun conf(context: ApplicationContext): RedisConf? {
        return internalStore[context]?.second
    }

    internal fun client(context: ApplicationContext): JedisPooled? {
        return internalStore[context]?.third
    }
}