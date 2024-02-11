package io.github.tobi.laa.spring.boot.embedded.redis

import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConf
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Scope
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import redis.embedded.Redis
import java.util.*
import kotlin.random.Random

/**
 * Used for sharing common test logic across different test classes.
 */
@Component
@Scope("prototype")
internal open class RedisTests(
    val props: RedisProperties,
    val template: RedisTemplate<String, Any>,
    val random: Random = Random.Default
) : ApplicationContextAware {

    var testdata: Map<String, Any> = emptyMap()

    var context: ApplicationContext? = null

    fun nothing(): RedisTests {
        return this
    }

    fun and(): RedisTests {
        return this
    }

    fun randomTestdata(): RedisTests {
        testdata = mapOf(
            randomKey() to UUID.randomUUID().toString(),
            randomKey() to random.nextLong(),
            randomKey() to random.nextBoolean(),
            randomKey() to random.nextBytes(8)
        )
        return this
    }

    private fun randomKey() = UUID.randomUUID().toString()

    fun whenDoingNothing(): RedisTests {
        return this
    }

    fun whenRedis(): Redis {
        return Redis()
    }

    fun then(): RedisTests {
        return this
    }

    fun redis(): Redis {
        return Redis()
    }

    fun redisProperties(): RedisPropertiesAssertion {
        return RedisPropertiesAssertion()
    }

    fun embeddedRedis(): EmbeddedRedis {
        return EmbeddedRedis()
    }

    override fun setApplicationContext(applicationContext: ApplicationContext) {
        this.context = applicationContext
    }

    inner class Redis {

        fun isBeingWrittenTo(): Redis {
            testdata.forEach { (key, value) -> template.opsForValue().set(key, value) }
            return this
        }

        fun then(): RedisTests {
            return this@RedisTests
        }

        fun shouldContainTheTestdata(): Redis {
            testdata.forEach { (key, value) -> assertThat(template.opsForValue().get(key)).isEqualTo(value) }
            return this
        }

        fun shouldNotContainAnyTestdata(): Redis {
            testdata.forEach { (key, _) -> assertThat(template.opsForValue().get(key)).isNull() }
            return this
        }
    }

    inner class RedisPropertiesAssertion :
        ObjectAssert<RedisProperties>(props) {

        fun and(): RedisPropertiesAssertion {
            return this
        }

        fun andAlso(): RedisTests {
            return this@RedisTests
        }

        fun shouldHaveHost(host: String): RedisPropertiesAssertion {
            assertThat(props.host).isEqualTo(host)
            return this
        }

        fun shouldHavePort(port: Int): RedisPropertiesAssertion {
            assertThat(props.port).isEqualTo(port)
            return this
        }

        fun shouldBeCluster(): RedisPropertiesAssertion {
            assertThat(props.cluster).isNotNull()
            return this
        }

        fun shouldBeStandalone(): RedisPropertiesAssertion {
            assertThat(props.cluster).isNull()
            return this
        }
    }

    inner class EmbeddedRedis {

        fun andAlso(): RedisTests {
            return this@RedisTests
        }

        @Suppress("UNCHECKED_CAST")
        fun shouldHaveConfig(): RedisConfAssertion {
            val conf = context?.let { RedisStore.conf(it) }!!
            return RedisConfAssertion(conf)
        }
    }

    inner class RedisConfAssertion(val conf: RedisConf) {

        fun and(): RedisConfAssertion {
            return this
        }

        fun andAlso(): RedisTests {
            return this@RedisTests
        }

        fun thatContainsDirective(keyword: String, vararg arguments: String): RedisConfAssertion {
            return thatContainsDirective(RedisConf.Directive(keyword, *arguments))
        }

        fun thatContainsDirective(directive: RedisConf.Directive): RedisConfAssertion {
            assertThat(conf.directives).contains(directive)
            return this
        }
    }
}