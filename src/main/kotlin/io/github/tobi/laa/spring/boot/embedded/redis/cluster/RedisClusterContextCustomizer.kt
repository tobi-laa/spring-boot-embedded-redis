package io.github.tobi.laa.spring.boot.embedded.redis.cluster

import io.github.tobi.laa.spring.boot.embedded.redis.*
import io.github.tobi.laa.spring.boot.embedded.redis.birds.BirdNameProvider
import io.github.tobi.laa.spring.boot.embedded.redis.ports.PortProvider
import org.slf4j.LoggerFactory
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextClosedEvent
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.MergedContextConfiguration
import redis.clients.jedis.HostAndPort
import redis.clients.jedis.JedisCluster
import redis.embedded.RedisServer
import redis.embedded.RedisServer.newRedisServer
import redis.embedded.RedisShardedCluster
import redis.embedded.core.ExecutableProvider.newJarResourceProvider
import redis.embedded.core.RedisServerBuilder
import java.io.File
import java.time.Duration
import java.util.stream.IntStream.range
import kotlin.reflect.full.createInstance
import kotlin.streams.toList

private const val CLUSTER_IP = "127.0.0.1"

internal class RedisClusterContextCustomizer(
    private val config: EmbeddedRedisCluster,
    private val portProvider: PortProvider = PortProvider()
) : ContextCustomizer {

    private val log = LoggerFactory.getLogger(javaClass)

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

    private fun createAndStartCluster(): RedisShardedCluster {
        val cluster = createCluster()
        cluster.start()
        log.info("Started Redis sharded cluster on ports ${cluster.ports()}")
        return cluster
    }

    private fun createCluster(): RedisShardedCluster {
        val ports = ports().iterator()
        val shards = config.shards.map { createShard(it, ports) }
        val nodes = shards.map { it.second + it.first }.flatten().toList()
        val replicasPortsByMainNodePort =
            shards.associate { s -> s.first.ports().first() to s.second.map { it.ports() }.flatten().toSet() }
        val cluster =
            RedisShardedCluster(nodes, replicasPortsByMainNodePort, Duration.ofSeconds(config.initializationTimeout))
        return cluster
    }

    private fun ports(): List<Int> {
        return if (config.ports.isEmpty()) {
            val nOfNodes = config.shards.sumOf { it.replicas + 1 }
            range(0, nOfNodes).map { _ -> portProvider.next() }.toList()
        } else {
            config.ports.map { if (it == 0) unspecifiedUnusedPort() else it }.toList()
        }
    }

    private fun unspecifiedUnusedPort(): Int {
        var port = portProvider.next()
        while (port in config.ports) {
            port = portProvider.next()
        }
        return port
    }

    private fun createShard(
        shard: EmbeddedRedisCluster.Shard,
        ports: Iterator<Int>
    ): Pair<RedisServer, List<RedisServer>> {
        val name = shard.name.ifEmpty { BirdNameProvider.next() }

        val mainNodeBuilder = createNodeBuilder(ports.next())
        customizer.forEach { c -> c.customizeMainNode(mainNodeBuilder, config, name) }

        val replicaBuilders = 1.rangeTo(shard.replicas).map { _ -> createNodeBuilder(ports.next()) }.toList()
        customizer.forEach { c -> c.customizeReplicas(replicaBuilders, config, name) }

        return mainNodeBuilder.build() to replicaBuilders.map { it.build() }
    }

    private fun createNodeBuilder(port: Int): RedisServerBuilder {
        val builder = newRedisServer()
            .bind(CLUSTER_IP)
            .port(port)
            .setting("cluster-enabled yes")
            .setting("cluster-config-file nodes-replica-$port.conf")
            .setting("cluster-node-timeout 5000")
            .setting("appendonly no")
        if (config.executeInDirectory.isNotEmpty()) {
            builder.executableProvider(newJarResourceProvider(File(config.executeInDirectory)))
        }
        return builder
    }

    private fun parseAddresses(cluster: RedisShardedCluster): List<Pair<String, Int>> =
        cluster.servers()
            .map { CLUSTER_IP to it.ports().first() }
            .toList()

    private fun createClient(addresses: List<Pair<String, Int>>): RedisClient {
        val jedisCluster = JedisCluster(addresses.map { HostAndPort(it.first, it.second) }.toSet())
        return RedisClusterClient(jedisCluster)
    }

    private fun setSpringProperties(context: ConfigurableApplicationContext, addresses: List<Pair<String, Int>>) {
        TestPropertyValues.of(
            mapOf(
                "spring.data.redis.cluster.nodes" to addresses.joinToString(",") { createAddress(it.first, it.second) }
            )
        ).applyTo(context.environment)
    }

    private fun addShutdownListener(
        context: ConfigurableApplicationContext,
        cluster: RedisShardedCluster,
        client: RedisClient
    ) {
        context.addApplicationListener { event ->
            if (event is ContextClosedEvent) {
                closeSafely(client)
                log.info("Stopping Redis sharded clusters on ports ${cluster.ports()}")
                cluster.servers().forEach { stopSafely(it) }
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
}