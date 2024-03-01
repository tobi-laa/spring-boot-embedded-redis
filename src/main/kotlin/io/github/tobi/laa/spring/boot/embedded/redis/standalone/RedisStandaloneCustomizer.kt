package io.github.tobi.laa.spring.boot.embedded.redis.standalone

import redis.embedded.core.RedisServerBuilder
import java.util.function.BiConsumer

/**
 * Can be implemented to customize how the standalone embedded Redis server is built.
 *
 * Implementations _must_ have a no-arg constructor.
 *
 * @see EmbeddedRedisStandalone.customizer
 */
interface RedisStandaloneCustomizer : BiConsumer<RedisServerBuilder, EmbeddedRedisStandalone>