package io.github.tobi.laa.spring.boot.embedded.redis.shardedcluster

import io.github.tobi.laa.spring.boot.embedded.redis.junit.extension.RedisFlushAllExtension
import io.github.tobi.laa.spring.boot.embedded.redis.server.RedisServerContextCustomizerFactory
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.ContextCustomizerFactories

/**
 * Annotation to enable an [embedded Redis sharded cluster][redis.embedded.RedisShardedCluster] for tests.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ExtendWith(RedisFlushAllExtension::class)
@ContextCustomizerFactories(RedisServerContextCustomizerFactory::class)
internal annotation class EmbeddedRedisShardedCluster
