package io.github.tobi.laa.spring.boot.embedded.redis.highavailability

import io.github.tobi.laa.spring.boot.embedded.redis.findTestClassAnnotations
import org.springframework.test.context.ContextConfigurationAttributes
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.ContextCustomizerFactory

internal class RedisHighAvailabilityContextCustomizerFactory : ContextCustomizerFactory {

    override fun createContextCustomizer(
        testClass: Class<*>,
        configAttributes: MutableList<ContextConfigurationAttributes>
    ): ContextCustomizer {
        val embeddedRedisHighAvailability =
            findTestClassAnnotations(testClass, EmbeddedRedisHighAvailability::class.java).firstOrNull()
        return RedisHighAvailabilityContextCustomizer(embeddedRedisHighAvailability!!)
    }
}