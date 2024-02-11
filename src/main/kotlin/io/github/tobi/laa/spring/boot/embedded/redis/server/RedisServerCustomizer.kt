package io.github.tobi.laa.spring.boot.embedded.redis.server

import redis.embedded.core.RedisServerBuilder
import java.util.function.BiConsumer

/**
 * Can be implemented to customize how the Redis server is built.
 *
 * Implementations _must_ have a no-arg constructor.
 *
 * @see EmbeddedRedisServer.customizer
 */
interface RedisServerCustomizer : BiConsumer<RedisServerBuilder, EmbeddedRedisServer>