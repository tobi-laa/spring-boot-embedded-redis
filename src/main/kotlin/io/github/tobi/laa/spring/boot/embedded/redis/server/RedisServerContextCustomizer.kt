package io.github.tobi.laa.spring.boot.embedded.redis.server

import io.github.tobi.laa.spring.boot.embedded.redis.PortProvider
import io.github.tobi.laa.spring.boot.embedded.redis.RedisStore
import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConf
import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConfLocator
import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConfParser
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.event.ContextClosedEvent
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.MergedContextConfiguration
import redis.clients.jedis.JedisPooled
import redis.embedded.Redis
import redis.embedded.RedisServer
import redis.embedded.RedisServer.newRedisServer
import redis.embedded.core.ExecutableProvider.newJarResourceProvider
import java.io.File
import kotlin.reflect.full.createInstance

internal class RedisServerContextCustomizer(
    private val config: EmbeddedRedisServer,
    private val portProvider: PortProvider = PortProvider()
) : ContextCustomizer {

    override fun customizeContext(context: ConfigurableApplicationContext, mergedConfig: MergedContextConfiguration) {
        RedisStore.computeIfAbsent(context) {
            val server = createAndStartServer()
            val conf = parseConf(server)
            val client = createClient(server, conf)
            setSpringProperties(context, server, conf)
            addShutdownListener(context, server, client)
            Triple(server, conf, client)
        }
    }

    private fun createAndStartServer(): Redis {
        val server = createServer()
        server.start()
        return server
    }

    private fun createServer(): RedisServer {
        val builder = newRedisServer()
            .port(if (config.port != 0) config.port else portProvider.next())
        if (config.bind.isNotEmpty()) {
            builder.bind(config.bind)
        }
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

    private fun parseConf(server: Redis): RedisConf =
        RedisConfLocator.locate(server).let { return RedisConfParser.parse(it) }

    private fun createClient(server: Redis, conf: RedisConf): JedisPooled {
        return JedisPooled(conf.getBinds().first(), server.ports().first())
    }

    private fun setSpringProperties(context: ConfigurableApplicationContext, server: Redis, conf: RedisConf) {
        TestPropertyValues.of(
            mapOf(
                "spring.data.redis.port" to server.ports().first().toString(),
                "spring.data.redis.host" to conf.getBinds().first()
            )
        ).applyTo(context.environment)
    }

    private fun addShutdownListener(context: ConfigurableApplicationContext, server: Redis, client: JedisPooled) {
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

        other as RedisServerContextCustomizer

        return config == other.config
    }

    override fun hashCode(): Int {
        return config.hashCode()
    }
}