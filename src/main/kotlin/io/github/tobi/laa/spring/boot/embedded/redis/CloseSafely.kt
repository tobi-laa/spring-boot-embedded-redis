package io.github.tobi.laa.spring.boot.embedded.redis

import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("io.github.tobi.laa.spring.boot.embedded.redis")

internal fun closeSafely(closeable: AutoCloseable) {
    try {
        closeable.close()
    } catch (e: Exception) {
        logger.error("Failed to close $closeable", e)
    }
}