package io.github.tobi.laa.spring.boot.embedded.redis.junit.extension

import io.github.tobi.laa.spring.boot.embedded.redis.RedisFlushAll
import io.github.tobi.laa.spring.boot.embedded.redis.RedisFlushAll.Mode.AFTER_CLASS
import io.github.tobi.laa.spring.boot.embedded.redis.RedisFlushAll.Mode.AFTER_METHOD
import io.github.tobi.laa.spring.boot.embedded.redis.RedisStore
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.test.context.junit.jupiter.SpringExtension.getApplicationContext

/**
 * JUnit 5 extension to flush all Redis data after each test method or after all test methods of a test class.
 */
internal class RedisFlushAllExtension : AfterEachCallback, AfterAllCallback {

    override fun afterEach(extensionContext: ExtensionContext?) {
        if (flushAllMode(extensionContext) == AFTER_METHOD) {
            flushAll(extensionContext)
        }
    }

    override fun afterAll(extensionContext: ExtensionContext?) {
        if (flushAllMode(extensionContext) == AFTER_CLASS) {
            flushAll(extensionContext)
        }
    }

    private fun flushAllMode(extensionContext: ExtensionContext?): RedisFlushAll.Mode {
        return extensionContext!!.requiredTestClass!!.getAnnotation(RedisFlushAll::class.java)?.mode ?: AFTER_METHOD
    }

    private fun flushAll(extensionContext: ExtensionContext?) {
        val applicationContext = getApplicationContext(extensionContext!!)
        RedisStore.client(applicationContext)!!.flushAll()
    }
}