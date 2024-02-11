package io.github.tobi.laa.spring.boot.embedded.redis

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import redis.embedded.RedisInstance
import java.io.File
import java.util.*
import kotlin.random.Random

/**
 * Used for sharing common test logic across different test classes.
 */
@Component
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
            testdata.forEach { (key, value) -> assertThat(template.opsForValue().get(key)).isNull() }
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
        fun shouldHaveConfigFile(): ConfigFile {
            val redis = context?.let { RedisStore.get(it) }
            assertThat(redis).isNotNull

            val argsProp = RedisInstance::class.java.declaredFields.firstOrNull { it.name == "args" }
            assertThat(argsProp).isNotNull

            argsProp!!.isAccessible = true
            val args = argsProp.get(redis) as List<String>?
            assertThat(args).isNotNull

            val configFile = args!!.find { it.endsWith(".conf") }?.let { File(it) }
            assertThat(configFile).isNotNull().exists()

            return ConfigFile(configFile!!)
        }
    }

    inner class ConfigFile(file: File) {

        val settings = file.readLines()

        fun and(): ConfigFile {
            return this
        }

        fun andAlso(): RedisTests {
            return this@RedisTests
        }

        fun thatContainsSetting(setting: String): ConfigFile {
            assertThat(settings).contains(setting)
            return this
        }
    }
}