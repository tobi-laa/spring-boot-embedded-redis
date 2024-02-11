package io.github.tobi.laa.spring.boot.embedded.redis.ports

import redis.embedded.Redis.DEFAULT_REDIS_PORT
import redis.embedded.core.PortProvider.REDIS_CLUSTER_MAX_PORT_EXCLUSIVE
import java.util.concurrent.ConcurrentSkipListSet

/**
 * The default port used by Redis Sentinel.
 */
private const val DEFAULT_SENTINEL_PORT: Int = 26379

/**
 * This offset is used to calculate the port for the Redis cluster bus.
 */
private const val BUS_PORT_OFFSET: Int = 10000

/**
 * Is used for providing free ports for Redis instances.
 */
internal class PortProvider {

    private val handedOutPorts = ConcurrentSkipListSet<Int>()

    /**
     * Provides the next free port for a Redis instance.
     * @param sentinel If `true`, the next free port for a Redis Sentinel instance is provided.
     */
    fun next(sentinel: Boolean = false): Int {
        return if (sentinel) {
            next(DEFAULT_SENTINEL_PORT..REDIS_CLUSTER_MAX_PORT_EXCLUSIVE)
        } else {
            next(DEFAULT_REDIS_PORT..REDIS_CLUSTER_MAX_PORT_EXCLUSIVE)
        }
    }

    private fun next(candidates: IntRange): Int {
        for (candidate in candidates) {
            if (portCanBeHandedOut(candidate) && handedOutPorts.add(candidate)) {
                return candidate
            }
        }
        throw IllegalStateException("Could not find an available TCP port")
    }

    private fun portCanBeHandedOut(port: Int): Boolean {
        val busPort = port + BUS_PORT_OFFSET
        return !handedOutPorts.contains(port) && !handedOutPorts.contains(busPort) &&
                PortChecker.available(port) && PortChecker.available(busPort)
    }
}