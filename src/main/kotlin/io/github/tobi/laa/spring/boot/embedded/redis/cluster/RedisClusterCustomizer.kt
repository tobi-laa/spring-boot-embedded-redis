package io.github.tobi.laa.spring.boot.embedded.redis.cluster

import redis.embedded.core.RedisSentinelBuilder
import redis.embedded.core.RedisServerBuilder

/**
 * Can be implemented to customize how the replication groups and/or sentinels of the Redis cluster are built.
 *
 * Implementations _must_ have a no-arg constructor.
 *
 * @see EmbeddedRedisCluster.customizer
 */
interface RedisClusterCustomizer {

    /**
     * Customizes how the main node is built.
     *
     * @param builder The builder of the main node to customize.
     * @param config The configuration of the Redis cluster.
     */
    fun customizeMainNode(
        builder: RedisServerBuilder,
        config: EmbeddedRedisCluster
    )

    /**
     * Customizes how the replicas are built.
     *
     * @param builder The builders of the replicas to customize.
     * @param config The configuration of the Redis cluster.
     */
    fun customizeReplicas(
        builder: List<RedisServerBuilder>,
        config: EmbeddedRedisCluster
    )

    /**
     * Customizes how the sentinels of the Redis cluster are built.
     *
     * @param builder The builders of the sentinel to customize.
     * @param config The configuration of the Redis cluster.
     * @param sentinelConfig The configuration of the sentinel.
     */
    fun customizeSentinels(
        builder: RedisSentinelBuilder,
        config: EmbeddedRedisCluster,
        sentinelConfig: EmbeddedRedisCluster.Sentinel
    )
}