package io.github.tobi.laa.spring.boot.embedded.redis.cluster

import io.github.tobi.laa.spring.boot.embedded.redis.RedisStore
import io.github.tobi.laa.spring.boot.embedded.redis.birds.BirdNameProvider
import io.github.tobi.laa.spring.boot.embedded.redis.cluster.EmbeddedRedisCluster.ReplicationGroup
import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConfLocator
import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConfParser
import io.github.tobi.laa.spring.boot.embedded.redis.ports.PortProvider
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextClosedEvent
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.MergedContextConfiguration
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisCluster
import redis.clients.jedis.UnifiedJedis
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

private const val DEFAULT_BIND = "localhost"
private const val QUORUM_SIZE = (1 / 2) + 1 // quorom size for one main node

internal class RedisClusterContextCustomizer(
    private val config: EmbeddedRedisCluster,
    private val portProvider: PortProvider = PortProvider()
) : ContextCustomizer {

    private val manuallySpecifiedPorts =
        config.replicationGroups.map { it.ports.toList() }.flatten().filter { it != 0 }.toSet() +
                config.sentinels.map { it.port }.filter { it != 0 }.toSet()

    private val customizer = config.customizer.map { c -> c.createInstance() }.toList()

    override fun customizeContext(context: ConfigurableApplicationContext, mergedConfig: MergedContextConfiguration) {
        RedisStore.computeIfAbsent(context) {
            val cluster = createAndStartCluster()
            val addresses = parseAddresses(cluster)
            val client = createClient(addresses)
            setSpringProperties(context, addresses)
            addShutdownListener(context, cluster, client)
            Pair(cluster, client)
        }
    }

    private fun createAndStartCluster(): RedisCluster {
        val cluster = createCluster()
        cluster.start()
        return cluster
    }

    private fun createCluster(): RedisCluster {
        val replicationGroups = config.replicationGroups.map { createReplicationGroup(it) }.associateBy { it.first }
        val sentinels = config.sentinels.map { createSentinel(it, replicationGroups) }
        return RedisCluster(sentinels, replicationGroups.values.flatMap { it.third + it.second.node }.toList())
    }

    private fun createReplicationGroup(groupConfig: ReplicationGroup): Triple<String, Node, List<RedisServer>> {
        val group = Group(
            groupConfig.name.ifEmpty { BirdNameProvider.next() },
            ports(groupConfig).iterator(),
            binds(groupConfig).iterator(),
            groupConfig
        )

        val mainNode = createAndStartMainNode(group)
        val replicaBuilders =
            IntStream.range(0, groupConfig.replicas)
                .mapToObj { _ -> createReplicaBuilder(mainNode, group) }
                .toList()
        customizer.forEach { c -> c.customizeReplicas(replicaBuilders, config, group.name) }

        return Triple(group.name, mainNode, replicaBuilders.map { it.build() })
    }

    private fun createAndStartMainNode(group: Group): Node {
        val builder = RedisServer.newRedisServer()
            .bind(group.binds.next())
            .port(group.ports.next())
        if (config.executeInDirectory.isNotEmpty()) {
            builder.executableProvider(ExecutableProvider.newJarResourceProvider(File(config.executeInDirectory)))
        }
        customizer.forEach { c -> c.customizeMainNode(builder, config, group.name) }
        val mainNode = builder.build()
        mainNode.start()
        return Node(mainNode)
    }

    private fun createReplicaBuilder(
        mainNode: Node,
        group: Group
    ): RedisServerBuilder {
        val builder = RedisServer.newRedisServer()
            .bind(group.binds.next())
            .port(group.ports.next())
            .slaveOf(mainNode.bind, mainNode.port)
        if (config.executeInDirectory.isNotEmpty()) {
            builder.executableProvider(ExecutableProvider.newJarResourceProvider(File(config.executeInDirectory)))
        }
        return builder
    }

    private fun ports(group: ReplicationGroup): List<Int> {
        return if (group.ports.isEmpty()) {
            val nOfNodes = group.replicas + 1
            IntStream.range(0, nOfNodes).map { _ -> portProvider.next() }.toList()
        } else {
            group.ports.map { if (it == 0) unspecifiedUnusedPort() else it }.toList()
        }
    }

    private fun unspecifiedUnusedPort(): Int {
        var port = portProvider.next()
        while (port in manuallySpecifiedPorts) {
            port = portProvider.next()
        }
        return port
    }

    private fun binds(group: ReplicationGroup): List<String> {
        return if (group.binds.isEmpty()) {
            val nOfNodes = group.replicas + 1
            IntStream.range(0, nOfNodes).mapToObj { _ -> DEFAULT_BIND }.toList()
        } else {
            group.binds.map { it.ifEmpty { DEFAULT_BIND } }.toList()
        }
    }

    private fun createSentinel(
        sentinelConfig: EmbeddedRedisCluster.Sentinel,
        replicationGroups: Map<String, Triple<String, Node, List<RedisServer>>>
    ): RedisSentinel {
        val builder = RedisSentinel.newRedisSentinel()
            .bind(sentinelConfig.bind.ifEmpty { DEFAULT_BIND })
            .port(if (sentinelConfig.port == 0) portProvider.next() else sentinelConfig.port)
        val monitoredGroups = if (sentinelConfig.monitoredGroups.isEmpty()) {
            replicationGroups.keys
        } else {
            sentinelConfig.monitoredGroups.toSet()
        }
        monitoredGroups.forEach { name ->
            val (_, mainNode, _) = replicationGroups[name]
                ?: throw IllegalStateException("No such replication group: $name")
            builder
                .setting("sentinel monitor $name ${mainNode.bind} ${mainNode.port} $QUORUM_SIZE")
                .setting("sentinel down-after-milliseconds $name ${sentinelConfig.downAfterMillis}")
                .setting("sentinel failover-timeout $name ${sentinelConfig.failOverTimeoutMillis}")
                .setting("sentinel parallel-syncs $name ${sentinelConfig.parallelSyncs}")
        }
        if (config.executeInDirectory.isNotEmpty()) {
            builder.executableProvider(ExecutableProvider.newJarResourceProvider(File(config.executeInDirectory)))
        }
        return builder.build()
    }

    private fun parseAddresses(cluster: RedisCluster): List<Pair<String, Int>> =
        cluster.servers()
            .map { parseBindAddress(it) to it.ports().first() }
            .toList()

    private fun createClient(addresses: List<Pair<String, Int>>): JedisCluster {
        return JedisCluster(addresses.map { HostAndPort(it.first, it.second) }.toSet())
    }

    private fun setSpringProperties(context: ConfigurableApplicationContext, addresses: List<Pair<String, Int>>) {
        TestPropertyValues.of(
            mapOf(
                "spring.data.redis.cluster.nodes" to addresses.joinToString(",") { "${it.first}:${it.second}" }
            )
        ).applyTo(context.environment)
    }

    private fun addShutdownListener(context: ConfigurableApplicationContext, server: Redis, client: UnifiedJedis) {
        context.addApplicationListener { event ->
            if (event is ContextClosedEvent) {
                client.close()
                server.stop()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RedisClusterContextCustomizer

        return config == other.config
    }

    override fun hashCode(): Int {
        return config.hashCode()
    }

    internal data class Group(
        val name: String,
        val ports: Iterator<Int>,
        val binds: Iterator<String>,
        val config: ReplicationGroup
    )

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