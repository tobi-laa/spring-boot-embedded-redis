package io.github.tobi.laa.spring.boot.embedded.redis.junit.extension

import org.junit.jupiter.api.extension.ExtendWith

/**
 * Compound annotation to enable all JUnit 5 extensions for embedded Redis tests.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@ExtendWith(RedisValidationExtension::class)
@ExtendWith(RedisParameterResolver::class)
@ExtendWith(RedisFlushAllExtension::class)
internal annotation class EmbeddedRedisTest
