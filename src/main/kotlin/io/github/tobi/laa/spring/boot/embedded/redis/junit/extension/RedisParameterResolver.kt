package io.github.tobi.laa.spring.boot.embedded.redis.junit.extension

import io.github.tobi.laa.spring.boot.embedded.redis.RedisStore
import io.github.tobi.laa.spring.boot.embedded.redis.cluster.EmbeddedRedisCluster
import io.github.tobi.laa.spring.boot.embedded.redis.server.EmbeddedRedisServer
import io.github.tobi.laa.spring.boot.embedded.redis.shardedcluster.EmbeddedRedisShardedCluster
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.springframework.test.context.junit.jupiter.SpringExtension.getApplicationContext
import redis.embedded.Redis
import redis.embedded.RedisCluster
import redis.embedded.RedisServer
import redis.embedded.RedisShardedCluster

/**
 * JUnit 5 extension to resolve [Redis] parameters.
 */
internal class RedisParameterResolver : ParameterResolver {

    override fun supportsParameter(parameterContext: ParameterContext?, extensionContext: ExtensionContext?): Boolean {
        val type = parameterType(parameterContext)
        return redisResolvable(type)
                || redisServerResolvable(type, extensionContext)
                || clusterResolvable(type, extensionContext)
                || shardedClusterResolvable(type, extensionContext)
    }

    private fun parameterType(parameterContext: ParameterContext?): Class<*> {
        return parameterContext!!.parameter.type
    }

    private fun redisResolvable(type: Class<*>): Boolean {
        return type == Redis::class.java
    }

    private fun redisServerResolvable(type: Class<*>, extensionContext: ExtensionContext?): Boolean {
        return type.isAssignableFrom(RedisServer::class.java)
                && annotatedWith(extensionContext, EmbeddedRedisServer::class.java)
    }

    private fun clusterResolvable(type: Class<*>, extensionContext: ExtensionContext?): Boolean {
        return type.isAssignableFrom(RedisCluster::class.java)
                && annotatedWith(extensionContext, EmbeddedRedisCluster::class.java)
    }

    private fun shardedClusterResolvable(type: Class<*>, extensionContext: ExtensionContext?): Boolean {
        return type.isAssignableFrom(RedisShardedCluster::class.java)
                && annotatedWith(extensionContext, EmbeddedRedisShardedCluster::class.java)
    }

    private fun annotatedWith(extensionContext: ExtensionContext?, annotationType: Class<out Annotation>): Boolean {
        return extensionContext!!.requiredTestClass.isAnnotationPresent(annotationType)
    }

    override fun resolveParameter(parameterContext: ParameterContext?, extensionContext: ExtensionContext?): Any {
        val applicationContext = getApplicationContext(extensionContext!!)
        return RedisStore.server(applicationContext)!!
    }
}