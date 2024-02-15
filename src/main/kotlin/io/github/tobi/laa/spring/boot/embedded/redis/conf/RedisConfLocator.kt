package io.github.tobi.laa.spring.boot.embedded.redis.conf

import redis.embedded.Redis
import redis.embedded.RedisInstance
import java.nio.file.Path
import java.nio.file.Paths

private val ARGS_PROP = RedisInstance::class.java.declaredFields
    .filter { it.name == "args" }
    .map { field -> field.isAccessible = true; field }
    .first()

/**
 * Locates the temporary Redis configuration file created by an embedded Redis server.
 */
internal object RedisConfLocator {

    @Suppress("UNCHECKED_CAST")
    internal fun locate(server: Redis): Path {
        val args = ARGS_PROP[server as RedisInstance] as List<String>
        return args.find { it.endsWith(".conf") }?.let { Paths.get(it) }
            ?: throw IllegalStateException("No config file found for embedded Redis server: $server")
    }
}