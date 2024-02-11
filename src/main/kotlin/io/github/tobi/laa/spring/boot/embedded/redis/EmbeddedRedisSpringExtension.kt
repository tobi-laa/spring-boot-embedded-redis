package io.github.tobi.laa.spring.boot.embedded.redis

import io.github.tobi.laa.spring.boot.embedded.redis.RedisFlushAll.Mode.AFTER_CLASS
import io.github.tobi.laa.spring.boot.embedded.redis.RedisFlushAll.Mode.AFTER_METHOD
import org.junit.jupiter.api.extension.*
import org.springframework.test.context.junit.jupiter.SpringExtension

internal class EmbeddedRedisSpringExtension : BeforeAllCallback, AfterEachCallback, AfterAllCallback,
    ParameterResolver {

    override fun beforeAll(context: ExtensionContext?) {
        // TODO("Not yet implemented")
    }

    override fun supportsParameter(parameterContext: ParameterContext?, extensionContext: ExtensionContext?): Boolean {
        // TODO("Not yet implemented")
        return false
    }

    override fun resolveParameter(parameterContext: ParameterContext?, extensionContext: ExtensionContext?): Any {
        // TODO("Not yet implemented")
        return Any()
    }

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
        val applicationContext = SpringExtension.getApplicationContext(extensionContext!!)
        RedisStore.client(applicationContext)!!.flushAll()
    }
}