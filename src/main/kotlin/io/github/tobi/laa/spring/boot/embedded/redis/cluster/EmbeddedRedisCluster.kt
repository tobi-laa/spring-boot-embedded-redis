package io.github.tobi.laa.spring.boot.embedded.redis.cluster

import io.github.tobi.laa.spring.boot.embedded.redis.junit.extension.EmbeddedRedisTest
import org.springframework.test.context.ContextCustomizerFactories
import kotlin.reflect.KClass

/**
 * Annotation to enable an [embedded Redis sharded cluster][redis.embedded.RedisShardedCluster] for tests.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@EmbeddedRedisTest
@ContextCustomizerFactories(RedisClusterContextCustomizerFactory::class)
annotation class EmbeddedRedisCluster(

    /**
     * The shards of the Redis cluster. Must contain at least one shard.
     *
     * @see redis.embedded.core.RedisShardedClusterBuilder.shard
     */
    val shards: Array<Shard> = [Shard()],

    /**
     * The ports to start the nodes of the embedded Redis cluster on. Can be left empty, in which case free ports
     * upwards from `6379` will be automatically used. Ports with the value `0` will be replaced with free ports.
     *
     * > **Warning**
     * If specified, the number of ports must be equal to the number of nodes in the cluster, that is, the sum of the
     * number of master and replica nodes.
     *
     * > **Warning**
     * Ports must not be specified more than once.
     *
     * > **Warning**
     * If a port is specified, the corresponding bus port must not be specified as well.
     *
     * @see redis.embedded.core.RedisShardedClusterBuilder.serverPorts
     */
    val ports: IntArray = [],

    /**
     * The time in seconds to wait for the Redis cluster to be initialized. Must be greater than 0.
     *
     * @see redis.embedded.core.RedisShardedClusterBuilder.initializationTimeout
     */
    val initializationTimeout: Long = 20,

    /**
     * The path to the directory to execute the Redis server nodes in. If set, the Redis executable will be executed in
     * the given directory. Applies to all nodes.
     *
     * @see redis.embedded.core.ExecutableProvider.newJarResourceProvider
     */
    val executeInDirectory: String = "",

    /**
     * Customizes how the shards of the Redis cluster are built. Customizers are executed by their order in this array.
     * Each customizer must have no-arg constructor.
     *
     * @see RedisShardCustomizer
     */
    val customizer: Array<KClass<out RedisShardCustomizer>> = []
) {

    /**
     * A single shard of the Redis cluster.
     */
    annotation class Shard(
        /**
         * The name of the shard. Only relevant for differentiating between shards when supplying customizers. If
         * nothing is set, the shard will be given the common english name of a random bird.
         */
        val name: String = "",

        /**
         * The number of replicas for the shard. Must be greater than 0.
         */
        val replicas: Int = 2
    )
}