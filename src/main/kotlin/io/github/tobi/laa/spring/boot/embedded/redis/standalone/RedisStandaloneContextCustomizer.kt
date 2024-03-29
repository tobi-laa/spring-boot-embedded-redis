package io.github.tobi.laa.spring.boot.embedded.redis.standalone

import io.github.tobi.laa.spring.boot.embedded.redis.RedisClient
import io.github.tobi.laa.spring.boot.embedded.redis.RedisStore
import io.github.tobi.laa.spring.boot.embedded.redis.closeSafely
import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConf
import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConfLocator
import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConfParser
import io.github.tobi.laa.spring.boot.embedded.redis.ports.PortProvider
import io.github.tobi.laa.spring.boot.embedded.redis.stopSafely
import org.slf4j.LoggerFactory
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

internal class RedisStandaloneContextCustomizer(
    private val config: EmbeddedRedisStandalone,
    private val portProvider: PortProvider = PortProvider()
) : ContextCustomizer {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun customizeContext(context: ConfigurableApplicationContext, mergedConfig: MergedContextConfiguration) {
        RedisStore.computeIfAbsent(context) {
            val server = createAndStartServer()
            val conf = parseConf(server)
            val client = createClient(server, conf)
            setSpringProperties(context, server, conf)
            addShutdownListener(context, server, client)
            Pair(server, client)
        }
    }

    private fun createAndStartServer(): Redis {
        val server = createServer()
        server.start()
        log.info("Started standalone Redis server on port ${server.ports().first()}")
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

    private fun createClient(server: Redis, conf: RedisConf): RedisClient {
        val jedisPooled = JedisPooled(conf.getBinds().first(), server.ports().first())
        return RedisStandaloneClient(jedisPooled)
    }

    private fun setSpringProperties(context: ConfigurableApplicationContext, server: Redis, conf: RedisConf) {
        TestPropertyValues.of(
            mapOf(
                "spring.data.redis.port" to server.ports().first().toString(),
                "spring.data.redis.host" to conf.getBinds().first()
            )
        ).applyTo(context.environment)
    }

    private fun addShutdownListener(context: ConfigurableApplicationContext, server: Redis, client: RedisClient) {
        context.addApplicationListener { event ->
            if (event is ContextClosedEvent) {
                closeSafely(client)
                log.info("Stopping standalone Redis server on port ${server.ports().first()}")
                stopSafely(server)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RedisStandaloneContextCustomizer

        return config == other.config
    }

    override fun hashCode(): Int {
        return config.hashCode()
    }
}