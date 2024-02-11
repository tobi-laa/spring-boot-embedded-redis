package io.github.tobi.laa.spring.boot.embedded.redis.junit.extension

import io.github.tobi.laa.spring.boot.embedded.redis.cluster.EmbeddedRedisCluster
import io.github.tobi.laa.spring.boot.embedded.redis.server.EmbeddedRedisServer
import io.github.tobi.laa.spring.boot.embedded.redis.shardedcluster.EmbeddedRedisShardedCluster
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * JUnit 5 extension to validate that the API is used correctly.
 */
internal class RedisValidationExtension : BeforeAllCallback {

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
        validatePort(config.port)
        require(config.configFile.isEmpty() || config.settings.isEmpty()) { "Either 'configFile' or 'settings' can be set, but not both" }
        require(config.customizer.all { it.constructors.any { it.parameters.isEmpty() } }) { "Customizers must have a no-arg constructor" }
    }

    private fun validateCluster(config: EmbeddedRedisCluster) {
        TODO()
    }

    private fun validateShardedCluster(config: EmbeddedRedisShardedCluster) {
        TODO()
    }

    private fun validatePort(port: Int) {
        require(port in 0..65535) { "Port must be in range 0..65535" }
    }

    private fun <A : Annotation> annotation(extensionContext: ExtensionContext?, type: Class<A>): A? {
        return extensionContext!!.requiredTestClass.getAnnotation(type)
    }
}