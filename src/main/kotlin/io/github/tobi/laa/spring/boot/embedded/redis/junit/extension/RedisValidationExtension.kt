package io.github.tobi.laa.spring.boot.embedded.redis.junit.extension

import io.github.tobi.laa.spring.boot.embedded.redis.cluster.EmbeddedRedisCluster
import io.github.tobi.laa.spring.boot.embedded.redis.server.EmbeddedRedisServer
import io.github.tobi.laa.spring.boot.embedded.redis.shardedcluster.EmbeddedRedisShardedCluster
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import kotlin.reflect.KClass

/**
 * JUnit 5 extension to validate that the API is used correctly.
 */
internal class RedisValidationExtension : BeforeAllCallback {

    private val VALID_PORT_RANGE = 0..65535

    override fun beforeAll(context: ExtensionContext?) {
        val embeddedRedisServer = annotation(context, EmbeddedRedisServer::class.java)
        val embeddedRedisCluster = annotation(context, EmbeddedRedisCluster::class.java)
        val embeddedRedisShardedCluster = annotation(context, EmbeddedRedisShardedCluster::class.java)
        validateMutuallyExclusive(embeddedRedisServer, embeddedRedisCluster, embeddedRedisShardedCluster)
        embeddedRedisServer?.let { validateServer(it) }
        embeddedRedisCluster?.let { validateCluster(it) }
        embeddedRedisShardedCluster?.let { validateShardedCluster(it) }
    }

    private fun validateMutuallyExclusive(
        embeddedRedisServer: EmbeddedRedisServer?,
        embeddedRedisCluster: EmbeddedRedisCluster?,
        embeddedRedisShardedCluster: EmbeddedRedisShardedCluster?
    ) {
        val count = listOfNotNull(embeddedRedisServer, embeddedRedisCluster, embeddedRedisShardedCluster).count()
        require(count <= 1) { "Only one of @EmbeddedRedisServer, @EmbeddedRedisCluster, @EmbeddedRedisShardedCluster is allowed" }
    }

    private fun validateServer(config: EmbeddedRedisServer) {
        require(config.port in VALID_PORT_RANGE) { "Port must be in range $VALID_PORT_RANGE" }
        require(config.configFile.isEmpty() || config.settings.isEmpty()) { "Either 'configFile' or 'settings' can be set, but not both" }
        validateCustomizers(config.customizer)
    }

    private fun validateCluster(config: EmbeddedRedisCluster) {
        TODO()
    }

    private fun validateShardedCluster(config: EmbeddedRedisShardedCluster) {
        require(config.shards.all { it.replicas > 0 }) { "Replicas for all shards must be greater than 0" }
        require(config.ports.isEmpty() || config.ports.size == config.shards.sumOf { it.replicas + 1 }) { "If ports are specified, they must match the number of nodes" }
        require(config.ports.all { it in VALID_PORT_RANGE }) { "All ports must be in range $VALID_PORT_RANGE" }
        require(config.initializationTimeout > 0) { "Initialization timeout must be greater than 0" }
        validateCustomizers(config.customizer)
    }

    private fun <T : Any> validateCustomizers(customizer: Array<KClass<out T>>) {
        require(customizer.all { haveNoArgConstructor(it) }) { "Customizers must have a no-arg constructor" }
    }

    private fun <T : Any> haveNoArgConstructor(customizer: KClass<out T>): Boolean {
        return customizer.constructors.any { it.parameters.isEmpty() }
    }

    private fun <A : Annotation> annotation(extensionContext: ExtensionContext?, type: Class<A>): A? {
        return extensionContext!!.requiredTestClass.getAnnotation(type)
    }
}