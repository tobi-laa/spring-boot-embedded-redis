package io.github.tobi.laa.spring.boot.embedded.redis.server

import io.github.tobi.laa.spring.boot.embedded.redis.findTestClassAnnotations
import org.springframework.test.context.ContextConfigurationAttributes
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.ContextCustomizerFactory

internal class RedisServerContextCustomizerFactory : ContextCustomizerFactory {

    override fun createContextCustomizer(
        testClass: Class<*>,
        configAttributes: MutableList<ContextConfigurationAttributes>
    ): ContextCustomizer {
        val embeddedRedisServer = findTestClassAnnotations(testClass, EmbeddedRedisServer::class.java).firstOrNull()
        return RedisServerContextCustomizer(embeddedRedisServer!!)
    }
}