package io.github.tobi.laa.spring.boot.embedded.redis

import org.slf4j.LoggerFactory
import redis.embedded.Redis

private val logger = LoggerFactory.getLogger("io.github.tobi.laa.spring.boot.embedded.redis")

internal fun stopSafely(redis: Redis) {
    try {
        redis.stop()
    } catch (e: Exception) {
        logger.error("Failed to stop Redis $redis", e)
    }
}