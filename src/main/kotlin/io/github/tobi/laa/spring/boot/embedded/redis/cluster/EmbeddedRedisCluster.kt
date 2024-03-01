package io.github.tobi.laa.spring.boot.embedded.redis.cluster

import io.github.tobi.laa.spring.boot.embedded.redis.junit.extension.EmbeddedRedisTest
import org.springframework.test.context.ContextCustomizerFactories
import kotlin.reflect.KClass

/**
 * Annotation to enable an [embedded Redis cluster][redis.embedded.RedisCluster] for tests.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@EmbeddedRedisTest
@ContextCustomizerFactories(RedisClusterContextCustomizerFactory::class)
annotation class EmbeddedRedisCluster( // FIXME rename to EmbeddedRedisHighAvailability

    /**
     * The name of the replication group. If nothing is set, the group will be given the common english name of a
     * random bird.
     */
    val name: String = "",

    /**
     * The number of (non-main) replicas. Must be greater than 0.
     */
    val replicas: Int = 2,

    /**
     * The ports to start the nodes on. Can be left empty, in which case free ports upwards from `6379` will be
     * automatically used. Ports with the value `0` will be replaced with free ports.
     *
     * The first port will be used for the main node, the rest for the replicas.
     *
     * > **Warning**
     * If specified, the number of ports must be equal to the number of nodes, that is, [replicas] + 1.
     *
     * > **Warning**
     * If specified, none of the ports may be specified for a sentinel.
     *
     * @see redis.embedded.core.RedisServerBuilder.port
     */
    val ports: IntArray = [],

    /**
     * The bind addresses to use. If set, the nodes will only bind to the given addresses. Can be left empty, in which
     * case the nodes will bind to `localhost`.
     *
     * The first address will be used for the main node, the rest for the replicas.
     *
     * > **Warning**
     * If specified, the number of addresses must be equal to the number of nodes, that is, [replicas] + 1.
     *
     * @see redis.embedded.core.RedisServerBuilder.bind
     */
    val binds: Array<String> = [],

    /**
     * The sentinels used to monitor the nodes. Must contain at least one sentinel.
     *
     * @see redis.embedded.RedisCluster.sentinels
     */
    val sentinels: Array<Sentinel> = [Sentinel()],

    /**
     * The path to the directory to execute the Redis server nodes and sentinels in. If set, the Redis executable will
     * be executed in the given directory. Applies to the nodes of all replication groups and all sentinels as well.
     *
     * @see redis.embedded.core.ExecutableProvider.newJarResourceProvider
     */
    val executeInDirectory: String = "",

    /**
     * Customizes how the replication groups and/or sentinels of the Redis cluster are built. Customizers are executed
     * by their order in this array.
     *
     * Each customizer must have no-arg constructor.
     *
     * @see RedisClusterCustomizer
     */
    val customizer: Array<KClass<out RedisClusterCustomizer>> = []
) {

    /**
     * A single sentinel.
     */
    annotation class Sentinel(

        /**
         * The port to start the embedded Redis sentinel on. If set to 0, a free port upwards from `26379` will be used.
         *
         * > **Warning**
         * If specified, the same port must not be specified for another sentinel or a node.
         *
         * @see redis.embedded.RedisSentinel.port
         */
        val port: Int = 0,

        /**
         * The bind address to use. If set, the sentinel will only bind to the given address.
         *
         * @see redis.embedded.core.RedisSentinelBuilder.bind
         */
        val bind: String = "localhost",

        /**
         * The time in milliseconds that a node has to be unreachable for to be considered down. Must be greater than 0.
         *
         * @see redis.embedded.core.RedisSentinelBuilder.downAfterMilliseconds
         */
        val downAfterMillis: Long = 60000,

        /**
         * The time in milliseconds that a failover process has to be completed within. Must be greater than 0.
         *
         * @see redis.embedded.core.RedisSentinelBuilder.failOverTimeout
         */
        val failOverTimeoutMillis: Long = 180000,

        /**
         * The number of replicas that Sentinel will try to reconfigure to use the new master node after a failover.
         * Must be greater than 0.
         *
         * @see redis.embedded.core.RedisSentinelBuilder.parallelSyncs
         */
        val parallelSyncs: Int = 1
    )
}