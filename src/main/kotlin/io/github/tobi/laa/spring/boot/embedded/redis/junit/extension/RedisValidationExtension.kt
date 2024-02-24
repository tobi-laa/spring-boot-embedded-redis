package io.github.tobi.laa.spring.boot.embedded.redis.junit.extension

import io.github.tobi.laa.spring.boot.embedded.redis.cluster.EmbeddedRedisCluster
import io.github.tobi.laa.spring.boot.embedded.redis.findTestClassAnnotations
import io.github.tobi.laa.spring.boot.embedded.redis.server.EmbeddedRedisServer
import io.github.tobi.laa.spring.boot.embedded.redis.shardedcluster.EmbeddedRedisShardedCluster
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.stream.Stream
import kotlin.reflect.KClass

/**
 * JUnit 5 extension to validate that the API is used correctly.
 */
internal class RedisValidationExtension : BeforeAllCallback {

    private val validPortRange = 0..65535

    override fun beforeAll(context: ExtensionContext?) {
        val embeddedRedisServer = annotation(context, EmbeddedRedisServer::class.java)
        val embeddedRedisCluster = annotation(context, EmbeddedRedisCluster::class.java)
        val embeddedRedisShardedCluster = annotation(context, EmbeddedRedisShardedCluster::class.java)
        validateMutuallyExclusive(context)
        embeddedRedisServer?.let { validateServer(it) }
        embeddedRedisCluster?.let { validateCluster(it) }
        embeddedRedisShardedCluster?.let { validateShardedCluster(it) }
    }

    private fun validateMutuallyExclusive(context: ExtensionContext?) {
        val count = Stream.of(
            annotations(context, EmbeddedRedisServer::class.java),
            annotations(context, EmbeddedRedisCluster::class.java),
            annotations(context, EmbeddedRedisShardedCluster::class.java)
        )
            .flatMap { it.stream() }
            .filter { it != null }
            .count()
        require(count <= 1) { "Only one of @EmbeddedRedisServer, @EmbeddedRedisCluster, @EmbeddedRedisShardedCluster is allowed" }
    }

    private fun validateServer(config: EmbeddedRedisServer) {
        require(config.port in validPortRange) { "Port must be in range $validPortRange" }
        require(config.configFile.isEmpty() || config.settings.isEmpty()) { "Either 'configFile' or 'settings' can be set, but not both" }
        validateCustomizers(config.customizer)
    }

    private fun validateCluster(config: EmbeddedRedisCluster) {
        TODO()
    }

    private fun validateShardedCluster(config: EmbeddedRedisShardedCluster) {
        require(config.shards.all { it.replicas > 0 }) { "Replicas for all shards must be greater than 0" }
        require(config.ports.isEmpty() || config.ports.size == config.shards.sumOf { it.replicas + 1 }) { "If ports are specified, they must match the number of nodes" }
        require(config.ports.all { it in validPortRange }) { "All ports must be in range $validPortRange" }
        require(config.initializationTimeout > 0) { "Initialization timeout must be greater than 0" }
        validateCustomizers(config.customizer)
    }

    private fun <T : Any> validateCustomizers(customizer: Array<KClass<out T>>) {
        require(customizer.all { haveNoArgConstructor(it) }) { "Customizers must have a no-arg constructor" }
    }

    private fun <T : Any> haveNoArgConstructor(customizer: KClass<out T>): Boolean {
        return customizer.constructors.any { it.parameters.isEmpty() }
    }

    private inline fun <reified A : Annotation> annotation(context: ExtensionContext?, type: Class<A>): A? {
        return annotations(context, type).firstOrNull()
    }

    private inline fun <reified A : Annotation> annotations(context: ExtensionContext?, type: Class<A>): List<A> {
        return findTestClassAnnotations(context!!.requiredTestClass, type)
    }
}