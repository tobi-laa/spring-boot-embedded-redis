package io.github.tobi.laa.spring.boot.embedded.redis.shardedcluster

import io.github.tobi.laa.spring.boot.embedded.redis.findTestClassAnnotation
import org.springframework.test.context.ContextConfigurationAttributes
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.ContextCustomizerFactory

internal class RedisShardedClusterContextCustomizerFactory : ContextCustomizerFactory {

    override fun createContextCustomizer(
        testClass: Class<*>,
        configAttributes: MutableList<ContextConfigurationAttributes>
    ): ContextCustomizer {
        val embeddedRedisShardedCluster = findTestClassAnnotation(testClass, EmbeddedRedisShardedCluster::class.java)
        return RedisShardedClusterContextCustomizer(embeddedRedisShardedCluster!!)
    }
}