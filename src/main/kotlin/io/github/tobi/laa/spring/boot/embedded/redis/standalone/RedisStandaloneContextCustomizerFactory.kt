package io.github.tobi.laa.spring.boot.embedded.redis.standalone

import io.github.tobi.laa.spring.boot.embedded.redis.findTestClassAnnotations
import org.springframework.test.context.ContextConfigurationAttributes
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.ContextCustomizerFactory

internal class RedisStandaloneContextCustomizerFactory : ContextCustomizerFactory {

    override fun createContextCustomizer(
        testClass: Class<*>,
        configAttributes: MutableList<ContextConfigurationAttributes>
    ): ContextCustomizer {
        val embeddedRedisStandalone =
            findTestClassAnnotations(testClass, EmbeddedRedisStandalone::class.java).firstOrNull()
        return RedisStandaloneContextCustomizer(embeddedRedisStandalone!!)
    }
}