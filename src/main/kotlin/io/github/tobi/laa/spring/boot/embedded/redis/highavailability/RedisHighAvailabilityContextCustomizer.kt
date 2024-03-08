package io.github.tobi.laa.spring.boot.embedded.redis.highavailability

import io.github.tobi.laa.spring.boot.embedded.redis.*
import io.github.tobi.laa.spring.boot.embedded.redis.birds.BirdNameProvider
import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConfLocator
import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConfParser
import io.github.tobi.laa.spring.boot.embedded.redis.ports.PortProvider
import org.slf4j.LoggerFactory
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
import redis.embedded.core.ExecutableProvider.newJarResourceProvider
import redis.embedded.core.ExecutableProvider.newProvidedVersionsMap
import redis.embedded.core.RedisServerBuilder
import redis.embedded.model.OsArchitecture.detectOSandArchitecture
import redis.embedded.util.IO.writeResourceToExecutableFile
import java.nio.file.Files.exists
import java.util.stream.IntStream
import kotlin.io.path.Path
import kotlin.reflect.full.createInstance
import kotlin.streams.toList

private const val DEFAULT_BIND = "127.0.0.1"
private const val QUORUM_SIZE = (1 / 2) + 1 // quorom size for one main node

internal class RedisHighAvailabilityContextCustomizer(
        private val config: EmbeddedRedisHighAvailability,
        private val portProvider: PortProvider = PortProvider()
) : ContextCustomizer {

    private val log = LoggerFactory.getLogger(javaClass)

    private val manuallySpecifiedPorts =
            config.ports.filter { it != 0 }.toSet() + config.sentinels.map { it.port }.filter { it != 0 }.toSet()
    private val name = config.name.ifEmpty { BirdNameProvider.next() }.replace(Regex("[^a-zA-Z0-9]"), "")
    private var nodeProvider: NodeProvider? = null
    private val customizer = config.customizer.map { c -> c.createInstance() }.toList()
    private val executableProvider = if (config.executeInDirectory.isNotEmpty()) {
        val osArch = detectOSandArchitecture()
        val resourcePath = newProvidedVersionsMap()[osArch]!!
        val executable = Path(config.executeInDirectory, resourcePath)
        ExecutableProvider {
            if (exists(executable)) {
                executable.toFile()
            } else {
                writeResourceToExecutableFile(executable.parent.toFile(), resourcePath)
            }
        }
    } else {
        newJarResourceProvider()
    }

    override fun customizeContext(context: ConfigurableApplicationContext, mergedConfig: MergedContextConfiguration) {
        RedisStore.computeIfAbsent(context) {
            val mainNode = createAndStartMainNode()
            try {
                val redisHighAvailability = createAndStartRedisInHighAvailabilityMode(mainNode)
                val sentinelAddresses = parseSentinelAddresses(redisHighAvailability)
                val client = createClient(sentinelAddresses)
                setSpringProperties(context, sentinelAddresses)
                addShutdownListener(context, redisHighAvailability, client)
                Pair(redisHighAvailability, client)
            } catch (e: Exception) {
                stopSafely(mainNode.second)
                throw e
            }
        }
    }

    private fun createAndStartRedisInHighAvailabilityMode(mainNode: Pair<Node, RedisServer>): RedisCluster {
        val redisHighAvailability = createRedisInHighAvailabilityMode(mainNode)
        redisHighAvailability.start()
        log.info("Started Redis in high availability mode on ports ${redisHighAvailability.ports()}")
        return redisHighAvailability
    }

    private fun createRedisInHighAvailabilityMode(mainNode: Pair<Node, RedisServer>): RedisCluster {
        val replicas = createReplicas(mainNode)
        val sentinels = config.sentinels.map { createSentinel(it, mainNode) }
        return RedisCluster(sentinels, replicas + mainNode.second)
    }

    private fun createReplicas(mainNode: Pair<Node, RedisServer>): List<RedisServer> {
        val replicaBuilders =
                IntStream.range(0, config.replicas)
                        .mapToObj { _ -> createReplicaBuilder(mainNode.first) }
                        .toList()
        customizer.forEach { c -> c.customizeReplicas(replicaBuilders, config) }

        return replicaBuilders.map { it.build() }
    }

    private fun createAndStartMainNode(): Pair<Node, RedisServer> {
        nodeProvider = nodeProvider()
        val nextNode = nodeProvider!!.next()
        val builder = RedisServer.newRedisServer()
                .bind(nextNode.bind)
                .port(nextNode.port)
        builder.executableProvider(executableProvider)
        customizer.forEach { c -> c.customizeMainNode(builder, config) }
        val mainNode = builder.build()
        mainNode.start()
        val port = mainNode.ports().first()
        log.info("Started Redis main node for high availability mode on port $port")
        return Node(mainNode) to mainNode
    }

    private fun createReplicaBuilder(
            mainNode: Node
    ): RedisServerBuilder {
        val nextNode = nodeProvider!!.next()
        val builder = RedisServer.newRedisServer()
                .bind(nextNode.bind)
                .port(nextNode.port)
                .slaveOf(mainNode.bind, mainNode.port)
        builder.executableProvider(executableProvider)
        return builder
    }

    private fun nodeProvider(): NodeProvider {
        val ports = if (config.ports.isEmpty()) {
            val nOfNodes = config.replicas + 1
            IntStream.range(0, nOfNodes).map { _ -> unspecifiedUnusedPort() }.toList()
        } else {
            config.ports.map { if (it == 0) unspecifiedUnusedPort() else it }.toList()
        }
        val binds = if (config.binds.isEmpty()) {
            val nOfNodes = config.replicas + 1
            IntStream.range(0, nOfNodes).mapToObj { _ -> DEFAULT_BIND }.toList()
        } else {
            config.binds.map { it.ifEmpty { DEFAULT_BIND } }.toList()
        }
        return NodeProvider(ports, binds)
    }

    private fun unspecifiedUnusedPort(sentinel: Boolean = false): Int {
        var port = portProvider.next(sentinel)
        while (port in manuallySpecifiedPorts) {
            port = portProvider.next(sentinel)
        }
        return port
    }

    private fun createSentinel(
            sentinelConfig: EmbeddedRedisHighAvailability.Sentinel,
            mainNode: Pair<Node, RedisServer>
    ): RedisSentinel {
        val builder = RedisSentinel.newRedisSentinel()
                .bind(sentinelConfig.bind.ifEmpty { DEFAULT_BIND })
                .port(if (sentinelConfig.port == 0) unspecifiedUnusedPort(true) else sentinelConfig.port)
                .setting("sentinel monitor $name ${mainNode.first.bind} ${mainNode.first.port} $QUORUM_SIZE")
                .setting("sentinel down-after-milliseconds $name ${sentinelConfig.downAfterMillis}")
                .setting("sentinel failover-timeout $name ${sentinelConfig.failOverTimeoutMillis}")
                .setting("sentinel parallel-syncs $name ${sentinelConfig.parallelSyncs}")
        builder.executableProvider(executableProvider)
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
        return RedisHighAvailabilityClient(jedisSentinelPool)
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
            redisHighAvailability: RedisCluster,
            client: RedisClient
    ) {
        context.addApplicationListener { event ->
            if (event is ContextClosedEvent) {
                closeSafely(client)
                log.info("Stopping Redis in high availability mode on ports ${redisHighAvailability.ports()}")
                redisHighAvailability.sentinels().forEach { stopSafely(it) }
                redisHighAvailability.servers().forEach { stopSafely(it) }
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

    internal class NodeProvider(private val addresses: Iterator<Pair<Int, String>>) {

        constructor(ports: List<Int>, binds: List<String>) : this(ports.zip(binds).iterator())

        fun next(): Node {
            val (port, bind) = addresses.next()
            return Node(port, bind)
        }
    }

    internal data class Node(val port: Int, val bind: String) {
        constructor(node: RedisServer) : this(
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