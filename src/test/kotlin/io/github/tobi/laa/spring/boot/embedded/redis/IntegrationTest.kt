package io.github.tobi.laa.spring.boot.embedded.redis

import org.springframework.boot.test.context.SpringBootTest

/**
 * Common annotation shared by all integration tests
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@SpringBootTest
annotation class IntegrationTest
