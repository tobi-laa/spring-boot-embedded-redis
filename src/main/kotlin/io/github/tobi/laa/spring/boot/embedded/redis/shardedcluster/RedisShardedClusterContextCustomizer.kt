package io.github.tobi.laa.spring.boot.embedded.redis.shardedcluster

import io.github.tobi.laa.spring.boot.embedded.redis.RedisStore
import io.github.tobi.laa.spring.boot.embedded.redis.birds.BirdNameProvider
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
import redis.embedded.RedisServer
import redis.embedded.RedisServer.newRedisServer
import redis.embedded.RedisShardedCluster
import redis.embedded.core.ExecutableProvider
import redis.embedded.core.RedisServerBuilder
import java.io.File
import java.time.Duration
import java.util.stream.IntStream.range
import kotlin.reflect.full.createInstance
import kotlin.streams.toList

private const val CLUSTER_IP = "127.0.0.1"

internal class RedisShardedClusterContextCustomizer(
    private val config: EmbeddedRedisShardedCluster,
    private val portProvider: PortProvider = PortProvider()
) : ContextCustomizer {

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
            val nOfNodes = config.shards.map { it.replicas + 1 }.sum()
            range(0, nOfNodes).map { _ -> portProvider.next() }.toList()
        } else {
            config.ports.asList()
        }
    }

    private fun createShard(
        shard: EmbeddedRedisShardedCluster.Shard,
        ports: Iterator<Int>
    ): Pair<RedisServer, List<RedisServer>> {
        val name = shard.name.ifEmpty { BirdNameProvider.next() }

        val mainNodeBuilder = createNodeBuilder(ports.next())
        config.customizer.forEach { c -> c.createInstance().customizeMainNode(mainNodeBuilder, config, name) }

        val replicaBuilders = range(0, shard.replicas).mapToObj { _ -> createNodeBuilder(ports.next()) }.toList()
        config.customizer.forEach { c -> c.createInstance().customizeReplicas(replicaBuilders, config, name) }

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
            builder.executableProvider(ExecutableProvider.newJarResourceProvider(File(config.executeInDirectory)))
        }
        return builder
    }

    private fun parseAddresses(cluster: RedisShardedCluster): List<Pair<String, Int>> =
        cluster.servers()
            .map { CLUSTER_IP to it.ports().first() }
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

        other as RedisShardedClusterContextCustomizer

        return config == other.config
    }

    override fun hashCode(): Int {
        return config.hashCode()
    }
}