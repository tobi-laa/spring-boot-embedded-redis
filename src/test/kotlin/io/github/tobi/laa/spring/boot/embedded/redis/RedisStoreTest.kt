package io.github.tobi.laa.spring.boot.embedded.redis

import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.context.ApplicationContext

@ExtendWith(MockKExtension::class)
@DisplayName("RedisStore tests")
internal class RedisStoreTest {

    @MockK
    private lateinit var context: ApplicationContext

    @Test
    @DisplayName("Unknown application context should yield null server")
    fun unknownAppContext_serverIsNull() {
        Assertions.assertThat(RedisStore.server(context)).isNull()
    }

    @Test
    @DisplayName("Unknown application context should yield null conf")
    fun unknownAppContext_confIsNull() {
        Assertions.assertThat(RedisStore.conf(context)).isNull()
    }

    @Test
    @DisplayName("Unknown application context should yield null client")
    fun unknownAppContext_clientIsNull() {
        Assertions.assertThat(RedisStore.client(context)).isNull()
    }
}