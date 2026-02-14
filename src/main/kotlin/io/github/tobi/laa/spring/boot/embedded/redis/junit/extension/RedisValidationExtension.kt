package io.github.tobi.laa.spring.boot.embedded.redis.junit.extension

import io.github.tobi.laa.spring.boot.embedded.redis.cluster.EmbeddedRedisCluster
import io.github.tobi.laa.spring.boot.embedded.redis.findTestClassAnnotations
import io.github.tobi.laa.spring.boot.embedded.redis.highavailability.EmbeddedRedisHighAvailability
import io.github.tobi.laa.spring.boot.embedded.redis.standalone.EmbeddedRedisStandalone
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.util.stream.Stream
import kotlin.reflect.KClass

/**
 * JUnit 5 extension to validate that the API is used correctly.
 */
internal class RedisValidationExtension : BeforeAllCallback {

    private val validPortRange = 0..65535

    override fun beforeAll(context: ExtensionContext) {
        val embeddedRedisStandalone = annotation(context, EmbeddedRedisStandalone::class.java)
        val embeddedRedisHighAvailability = annotation(context, EmbeddedRedisHighAvailability::class.java)
        val embeddedRedisCluster = annotation(context, EmbeddedRedisCluster::class.java)
        validateMutuallyExclusive(context)
        embeddedRedisStandalone?.let { validateStandalone(it) }
        embeddedRedisHighAvailability?.let { validateHighAvailability(it) }
        embeddedRedisCluster?.let { validateCluster(it) }
    }

    private fun validateMutuallyExclusive(context: ExtensionContext) {
        val count = Stream.of(
            annotations(context, EmbeddedRedisStandalone::class.java),
            annotations(context, EmbeddedRedisHighAvailability::class.java),
            annotations(context, EmbeddedRedisCluster::class.java)
        )
            .flatMap { it.stream() }
            .count()
        require(count <= 1) { "Only one of @EmbeddedRedisStandalone, @EmbeddedRedisHighAvailability, @EmbeddedRedisCluster is allowed" }
    }

    private fun validateStandalone(config: EmbeddedRedisStandalone) {
        require(config.port in validPortRange) { "Port must be in range $validPortRange" }
        require(config.configFile.isEmpty() || config.settings.isEmpty()) { "Either 'configFile' or 'settings' can be set, but not both" }
        validateCustomizers(config.customizer)
    }

    private fun validateHighAvailability(config: EmbeddedRedisHighAvailability) {
        validateReplicationGroups(config)
        validateSentinels(config)
        validatePorts(config)
        validateCustomizers(config.customizer)
    }

    private fun validateReplicationGroups(config: EmbeddedRedisHighAvailability) {
        require(config.replicas > 0) { "Replicas must be greater than 0" }
        require(config.ports.isEmpty() || config.ports.size == config.replicas + 1) { "If ports are specified, they must match the number of nodes" }
        require(config.ports.all { it in validPortRange }) { "All ports must be in range $validPortRange" }
    }

    private fun validateSentinels(config: EmbeddedRedisHighAvailability) {
        require(config.sentinels.isNotEmpty()) { "Sentinels must not be empty" }
        config.sentinels.forEach { sentinel ->
            require(sentinel.port in validPortRange) { "All ports must be in range $validPortRange" }
            require(sentinel.downAfterMillis > 0) { "Timeout for unreachable nodes must be greater than 0" }
            require(sentinel.failOverTimeoutMillis > 0) { "Failover timeout must be greater than 0" }
            require(sentinel.parallelSyncs > 0) { "Parallel syncs must be greater than 0" }
        }
    }

    private fun validatePorts(config: EmbeddedRedisHighAvailability) {
        val allPorts = config.ports.filter { it != 0 } + config.sentinels.map { it.port }.filter { it != 0 }
        require(allPorts.distinct().size == allPorts.size) { "Ports must not be specified more than once" }
    }

    private fun validateCluster(config: EmbeddedRedisCluster) {
        require(config.shards.isNotEmpty()) { "Shards must not be empty" }
        require(config.shards.all { it.replicas > 0 }) { "Replicas for all shards must be greater than 0" }
        require(config.ports.isEmpty() || config.ports.size == config.shards.sumOf { it.replicas + 1 }) { "If ports are specified, they must match the number of nodes" }
        require(config.ports.filter { it != 0 }
            .distinct().size == config.ports.filter { it != 0 }.size) { "Ports must not be specified more than once" }
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

    private inline fun <reified A : Annotation> annotation(context: ExtensionContext, type: Class<A>): A? {
        return annotations(context, type).firstOrNull()
    }

    private inline fun <reified A : Annotation> annotations(context: ExtensionContext, type: Class<A>): List<A> {
        return findTestClassAnnotations(context.requiredTestClass, type)
    }
}
