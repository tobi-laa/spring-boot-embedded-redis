package io.github.tobi.laa.spring.boot.embedded.redis.cluster

import redis.embedded.core.RedisServerBuilder

/**
 * Can be implemented to customize how the shards of the Redis cluster are built.
 *
 * Implementations _must_ have a no-arg constructor.
 *
 * @see EmbeddedRedisCluster.customizer
 */
interface RedisShardCustomizer {

    /**
     * Customizes how the main node of [shard] is built.
     *
     * @param builder The builder of the main node to customize.
     * @param config The configuration of the Redis cluster.
     * @param shard The name of the shard whose main node is being built.
     */
    fun customizeMainNode(
        builder: RedisServerBuilder,
        config: EmbeddedRedisCluster,
        shard: String
    )

    /**
     * Customizes how the replicase of [shard] are built.
     *
     * @param builder The builders of the replicas to customize.
     * @param config The configuration of the Redis cluster.
     * @param shard The name of the shard whose replicas are being built.
     */
    fun customizeReplicas(
        builder: List<RedisServerBuilder>,
        config: EmbeddedRedisCluster,
        shard: String
    )
}