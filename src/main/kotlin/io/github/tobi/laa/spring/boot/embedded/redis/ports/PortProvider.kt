package io.github.tobi.laa.spring.boot.embedded.redis.ports

import redis.embedded.Redis.DEFAULT_REDIS_PORT
import redis.embedded.core.PortProvider.REDIS_CLUSTER_MAX_PORT_EXCLUSIVE
import java.util.concurrent.ConcurrentSkipListSet

/**
 * The default port used by Redis Sentinel.
 */
internal const val DEFAULT_SENTINEL_PORT: Int = 26379

/**
 * This offset is used to calculate the port for the Redis cluster bus.
 */
internal const val BUS_PORT_OFFSET: Int = 10000

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
        val minPort = if (sentinel) DEFAULT_SENTINEL_PORT else DEFAULT_REDIS_PORT
        for (candidate in minPort until REDIS_CLUSTER_MAX_PORT_EXCLUSIVE + 1) {
            val candidateBusPort = candidate + BUS_PORT_OFFSET
            if (portCanBeHandedOut(candidate) && portCanBeHandedOut(candidateBusPort)) {
                handedOutPorts.add(candidate)
                handedOutPorts.add(candidateBusPort)
                return candidate
            }
        }
        throw IllegalStateException("Could not find an available TCP port")
    }

    private fun portCanBeHandedOut(port: Int): Boolean {
        return !handedOutPorts.contains(port) && PortChecker.available(port)
    }
}