package io.github.tobi.laa.spring.boot.embedded.redis.server

import io.github.tobi.laa.spring.boot.embedded.redis.findTestClassAnnotation
import org.springframework.test.context.ContextConfigurationAttributes
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.ContextCustomizerFactory

class RedisServerContextCustomizerFactory : ContextCustomizerFactory {

    override fun createContextCustomizer(
        testClass: Class<*>,
        configAttributes: MutableList<ContextConfigurationAttributes>
    ): ContextCustomizer? {
        val embeddedRedisServer = findTestClassAnnotation(testClass, EmbeddedRedisServer::class.java)
        return embeddedRedisServer?.let { RedisServerContextCustomizer(it) }
    }
}