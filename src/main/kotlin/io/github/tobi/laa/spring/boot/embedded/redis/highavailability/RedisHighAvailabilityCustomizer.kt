package io.github.tobi.laa.spring.boot.embedded.redis.highavailability

import redis.embedded.core.RedisSentinelBuilder
import redis.embedded.core.RedisServerBuilder

/**
 * Can be implemented to customize how the nodes and sentinels of embedded Redis in high availability mode are built.
 *
 * Implementations _must_ have a no-arg constructor.
 *
 * @see EmbeddedRedisHighAvailability.customizer
 */
interface RedisHighAvailabilityCustomizer {

    /**
     * Customizes how the main node is built.
     *
     * @param builder The builder of the main node to customize.
     * @param config The configuration of Redis in high availability mode.
     */
    fun customizeMainNode(
        builder: RedisServerBuilder,
        config: EmbeddedRedisHighAvailability
    )

    /**
     * Customizes how the replicas are built.
     *
     * @param builder The builders of the replicas to customize.
     * @param config The configuration of Redis in high availability mode.
     */
    fun customizeReplicas(
        builder: List<RedisServerBuilder>,
        config: EmbeddedRedisHighAvailability
    )

    /**
     * Customizes how the sentinels are built.
     *
     * @param builder The builders of the sentinel to customize.
     * @param config The configuration of Redis in high availability mode.
     * @param sentinelConfig The configuration of the sentinel.
     */
    fun customizeSentinels(
        builder: RedisSentinelBuilder,
        config: EmbeddedRedisHighAvailability,
        sentinelConfig: EmbeddedRedisHighAvailability.Sentinel
    )
}