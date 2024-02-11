package io.github.tobi.laa.spring.boot.embedded.redis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate

@SpringBootApplication
internal open class Application {

    companion object {
        @Bean
        fun objectTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
            return RedisTemplate<String, Any>().apply {
                setConnectionFactory(connectionFactory)
            }
        }
    }
}

internal fun main(args: Array<String>) {
    runApplication<Application>(*args)
}