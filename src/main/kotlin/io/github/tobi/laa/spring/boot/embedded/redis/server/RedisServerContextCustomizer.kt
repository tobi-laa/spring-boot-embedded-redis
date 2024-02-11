package io.github.tobi.laa.spring.boot.embedded.redis.server

import io.github.tobi.laa.spring.boot.embedded.redis.PortProvider
import io.github.tobi.laa.spring.boot.embedded.redis.RedisStore
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextClosedEvent
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.MergedContextConfiguration
import redis.embedded.Redis
import redis.embedded.RedisInstance
import redis.embedded.RedisServer
import redis.embedded.RedisServer.newRedisServer
import redis.embedded.core.ExecutableProvider.newJarResourceProvider
import java.io.File
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

internal class RedisServerContextCustomizer(
    val config: EmbeddedRedisServer,
    portProvider: PortProvider = PortProvider()
) : ContextCustomizer {

    val port = if (config.port != 0) config.port else portProvider.next()
    val host = if (config.bind.isNotEmpty()) config.bind else "127.0.0.1"

    override fun customizeContext(context: ConfigurableApplicationContext, mergedConfig: MergedContextConfiguration) {
        val redis = createAndStartServer(context)
        setSpringProperties(context, redis)
        addShutdownListener(context, redis)
    }

    private fun createAndStartServer(context: ConfigurableApplicationContext): Redis {
        val redis = RedisStore.computeIfAbsent(context) { createServer() }
        redis.start()
        return redis
    }

    private fun createServer(): RedisServer {
        val builder = newRedisServer()
            .port(port)
            .bind(host)
        if (config.configFile.isNotEmpty()) {
            builder.configFile(config.configFile)
        } else {
            config.settings.forEach { s -> builder.setting(s) }
        }
        if (config.executeInDirectory.isNotEmpty()) {
            builder.executableProvider(newJarResourceProvider(File(config.executeInDirectory)))
        }
        config.customizer.forEach { c -> c.createInstance().accept(builder, config) }
        return builder.build()
    }

    private fun setSpringProperties(context: ConfigurableApplicationContext, redis: Redis) {
        TestPropertyValues.of(
            mapOf(
                "spring.data.redis.port" to redis.ports().first().toString(),
                "spring.data.redis.host" to host
            )
        ).applyTo(context.environment)
    }

    @Suppress("UNCHECKED_CAST")
    private fun host(redis: Redis): String {
        val argsProperty = RedisInstance::class.memberProperties.firstOrNull { it.name == "args" }
        val args = argsProperty?.let {
            it.isAccessible = true
            it.get(redis as RedisInstance)
        } as List<String>
        println(args)
        return args.findLast { it.startsWith("bind") }?.split(" ")?.getOrNull(1) ?: "localhost"
    }

    private fun addShutdownListener(context: ConfigurableApplicationContext, redis: Redis) {
        context.addApplicationListener { event ->
            if (event is ContextClosedEvent) {
                redis.stop()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RedisServerContextCustomizer

        return config == other.config
    }

    override fun hashCode(): Int {
        return config.hashCode()
    }
}