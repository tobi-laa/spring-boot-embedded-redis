package io.github.tobi.laa.spring.boot.embedded.redis.server

import io.github.tobi.laa.spring.boot.embedded.redis.findTestClassAnnotation
import org.springframework.test.context.ContextConfigurationAttributes
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.ContextCustomizerFactory

internal class RedisServerContextCustomizerFactory : ContextCustomizerFactory {

    override fun createContextCustomizer(
        testClass: Class<*>,
        configAttributes: MutableList<ContextConfigurationAttributes>
    ): ContextCustomizer {
        val embeddedRedisServer = findTestClassAnnotation(testClass, EmbeddedRedisServer::class.java)
        return RedisServerContextCustomizer(embeddedRedisServer!!)
    }
}