package io.github.tobi.laa.spring.boot.embedded.redis.highavailability

import io.github.tobi.laa.spring.boot.embedded.redis.RedisClient
import io.github.tobi.laa.spring.boot.embedded.redis.RedisStore
import io.github.tobi.laa.spring.boot.embedded.redis.birds.BirdNameProvider
import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConfLocator
import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConfParser
import io.github.tobi.laa.spring.boot.embedded.redis.createAddress
import io.github.tobi.laa.spring.boot.embedded.redis.ports.PortProvider
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextClosedEvent
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.MergedContextConfiguration
import redis.clients.jedis.JedisSentinelPool
import redis.embedded.Redis
import redis.embedded.RedisCluster
import redis.embedded.RedisSentinel
import redis.embedded.RedisServer
import redis.embedded.core.ExecutableProvider
import redis.embedded.core.RedisServerBuilder
import java.io.File
import java.util.stream.IntStream
import kotlin.reflect.full.createInstance
import kotlin.streams.toList

private const val DEFAULT_BIND = "::1"
private const val QUORUM_SIZE = (1 / 2) + 1 // quorom size for one main node

internal class RedisHighAvailabilityContextCustomizer(
    private val config: EmbeddedRedisHighAvailability,
    private val portProvider: PortProvider = PortProvider()
) : ContextCustomizer {

    private val manuallySpecifiedPorts =
        config.ports.filter { it != 0 }.toSet() + config.sentinels.map { it.port }.filter { it != 0 }.toSet()
    private val name = config.name.ifEmpty { BirdNameProvider.next() }.replace(Regex("[^a-zA-Z0-9]"), "")
    private lateinit var nodePorts: Iterator<Int>
    private lateinit var nodeBinds: Iterator<String>
    private val customizer = config.customizer.map { c -> c.createInstance() }.toList()

    override fun customizeContext(context: ConfigurableApplicationContext, mergedConfig: MergedContextConfiguration) {
        RedisStore.computeIfAbsent(context) {
            val redisHighAvailability = createAndStartRedisInHighAvailabilityMode(context)
            val sentinelAddresses = parseSentinelAddresses(redisHighAvailability)
            val client = createClient(sentinelAddresses)
            setSpringProperties(context, sentinelAddresses)
            addShutdownListener(context, redisHighAvailability, client)
            Pair(redisHighAvailability, client)
        }
    }

    private fun createAndStartRedisInHighAvailabilityMode(context: ConfigurableApplicationContext): RedisCluster {
        nodePorts = ports().iterator()
        nodeBinds = binds().iterator()
        val redisHighAvailability = createRedisInHighAvailabilityMode(context)
        redisHighAvailability.start()
        return redisHighAvailability
    }

    private fun createRedisInHighAvailabilityMode(context: ConfigurableApplicationContext): RedisCluster {
        val replicationGroup = createReplicationGroup(context)
        val sentinels = config.sentinels.map { createSentinel(it, replicationGroup) }
        return RedisCluster(sentinels, replicationGroup.second + replicationGroup.first.node)
    }

    private fun createReplicationGroup(context: ConfigurableApplicationContext): Pair<Node, List<RedisServer>> {
        val mainNode = createAndStartMainNode(context)
        val replicaBuilders =
            IntStream.range(0, config.replicas)
                .mapToObj { _ -> createReplicaBuilder(mainNode) }
                .toList()
        customizer.forEach { c -> c.customizeReplicas(replicaBuilders, config) }

        return Pair(mainNode, replicaBuilders.map { it.build() })
    }

    private fun createAndStartMainNode(context: ConfigurableApplicationContext): Node {
        val builder = RedisServer.newRedisServer()
            .bind(nodeBinds.next())
            .port(nodePorts.next())
        if (config.executeInDirectory.isNotEmpty()) {
            builder.executableProvider(ExecutableProvider.newJarResourceProvider(File(config.executeInDirectory)))
        }
        customizer.forEach { c -> c.customizeMainNode(builder, config) }
        val mainNode = builder.build()
        mainNode.start()
        context.addApplicationListener { event ->
            if (event is ContextClosedEvent) {
                mainNode.stop()
            }
        }
        return Node(mainNode)
    }

    private fun createReplicaBuilder(
        mainNode: Node
    ): RedisServerBuilder {
        val builder = RedisServer.newRedisServer()
            .bind(nodeBinds.next())
            .port(nodePorts.next())
            .slaveOf(mainNode.bind, mainNode.port)
        if (config.executeInDirectory.isNotEmpty()) {
            builder.executableProvider(ExecutableProvider.newJarResourceProvider(File(config.executeInDirectory)))
        }
        return builder
    }

    private fun ports(): List<Int> {
        return if (config.ports.isEmpty()) {
            val nOfNodes = config.replicas + 1
            IntStream.range(0, nOfNodes).map { _ -> portProvider.next() }.toList()
        } else {
            config.ports.map { if (it == 0) unspecifiedUnusedPort() else it }.toList()
        }
    }

    private fun unspecifiedUnusedPort(): Int {
        var port = portProvider.next()
        while (port in manuallySpecifiedPorts) {
            port = portProvider.next()
        }
        return port
    }

    private fun binds(): List<String> {
        return if (config.binds.isEmpty()) {
            val nOfNodes = config.replicas + 1
            IntStream.range(0, nOfNodes).mapToObj { _ -> DEFAULT_BIND }.toList()
        } else {
            config.binds.map { it.ifEmpty { DEFAULT_BIND } }.toList()
        }
    }

    private fun createSentinel(
        sentinelConfig: EmbeddedRedisHighAvailability.Sentinel,
        replicationGroup: Pair<Node, List<RedisServer>>
    ): RedisSentinel {
        val mainNode = replicationGroup.first
        val builder = RedisSentinel.newRedisSentinel()
            .bind(sentinelConfig.bind.ifEmpty { DEFAULT_BIND })
            .port(if (sentinelConfig.port == 0) portProvider.next(true) else sentinelConfig.port)
            .setting("sentinel monitor $name ${mainNode.bind} ${mainNode.port} $QUORUM_SIZE")
            .setting("sentinel down-after-milliseconds $name ${sentinelConfig.downAfterMillis}")
            .setting("sentinel failover-timeout $name ${sentinelConfig.failOverTimeoutMillis}")
            .setting("sentinel parallel-syncs $name ${sentinelConfig.parallelSyncs}")
        if (config.executeInDirectory.isNotEmpty()) {
            builder.executableProvider(ExecutableProvider.newJarResourceProvider(File(config.executeInDirectory)))
        }
        customizer.forEach { c -> c.customizeSentinels(builder, config, sentinelConfig) }
        return builder.build()
    }

    private fun parseSentinelAddresses(redisHighAvailability: RedisCluster): List<Pair<String, Int>> =
        redisHighAvailability.sentinels()
            .map { parseBindAddress(it) to it.ports().first() }
            .toList()

    private fun createClient(sentinelAddresses: List<Pair<String, Int>>): RedisClient {
        val jedisSentinelPool =
            JedisSentinelPool(name, sentinelAddresses.map { createAddress(it.first, it.second) }.toSet())
        return JedisHighAvailabilityClient(jedisSentinelPool)
    }

    private fun setSpringProperties(
        context: ConfigurableApplicationContext,
        sentinelAddresses: List<Pair<String, Int>>
    ) {
        val nodes = sentinelAddresses.joinToString(",") {
            createAddress(
                it.first,
                it.second
            )
        }
        TestPropertyValues.of(
            mapOf(
                "spring.data.redis.sentinel.master" to name,
                "spring.data.redis.sentinel.nodes" to nodes
            )
        ).applyTo(context.environment)
    }

    private fun addShutdownListener(
        context: ConfigurableApplicationContext,
        redisHighAvailability: Redis,
        client: RedisClient
    ) {
        context.addApplicationListener { event ->
            if (event is ContextClosedEvent) {
                client.close()
                redisHighAvailability.stop()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RedisHighAvailabilityContextCustomizer

        return config == other.config
    }

    override fun hashCode(): Int {
        return config.hashCode()
    }

    internal data class Node(val node: RedisServer, val port: Int, val bind: String) {
        constructor(node: RedisServer) : this(
            node,
            node.ports().first(),
            parseBindAddress(node)
        )
    }

    private companion object {
        private fun parseBindAddress(node: Redis): String {
            return RedisConfLocator.locate(node).let { RedisConfParser.parse(it) }.getBinds().first()
        }
    }
}