package io.github.tobi.laa.spring.boot.embedded.redis

/**
 * A client to interact with the embedded Redis server.
 */
internal interface RedisClient : AutoCloseable {

    /**
     * Delete all the keys of all the existing databases, not just the currently selected one.
     */
    fun flushAll(): String

    /**
     * Gets the value for the specified [key] from the Redis server.
     * @param key the key to get the value for.
     * @return the value corresponding to [key] or null if the key does not exist.
     */
    fun get(key: String): String?
}