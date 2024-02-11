package io.github.tobi.laa.spring.boot.embedded.redis

import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.boot.test.context.SpringBootTest

/**
 * Common annotation shared by all integration tests
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
annotation class IntegrationTest
