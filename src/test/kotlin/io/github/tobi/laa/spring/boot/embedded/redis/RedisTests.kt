package io.github.tobi.laa.spring.boot.embedded.redis

import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConf
import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConfLocator
import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConfParser
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextAware
import org.springframework.context.annotation.Scope
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import redis.embedded.RedisCluster
import redis.embedded.RedisShardedCluster
import java.time.Duration
import java.util.*
import java.util.Collections.singletonList
import kotlin.random.Random

private val INITIALIZATION_TIMEOUT_PROP = RedisShardedCluster::class.java.declaredFields
    .filter { it.name == "initializationTimeout" }
    .map { field -> field.isAccessible = true; field }
    .first()

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

        fun shouldHaveNode(host: String, port: Int): RedisPropertiesAssertion {
            assertThat(props.cluster.nodes).contains("$host:$port")
            return this
        }
    }

    inner class EmbeddedRedis {

        fun andAlso(): RedisTests {
            return this@RedisTests
        }

        fun shouldHaveConfig(): RedisConfAssertion {
            val server = context?.let { RedisStore.server(it) }!!
            val conf =
                if (server is RedisShardedCluster) {
                    server.servers().map { RedisConfParser.parse(RedisConfLocator.locate(it)) }.toList()
                } else {
                    singletonList(RedisConfParser.parse(RedisConfLocator.locate(server)))
                }
            return RedisConfAssertion(conf)
        }

        fun shouldHaveInitializationTimeout(timeout: Duration): EmbeddedRedis {
            val server = context?.let { RedisStore.server(it) }!!
            assertThat(server).isInstanceOf(RedisShardedCluster::class.java)
            val configuredTimeout = INITIALIZATION_TIMEOUT_PROP[server as RedisShardedCluster]
            assertThat(configuredTimeout).isEqualTo(timeout)
            return this
        }

        fun shouldHaveSentinels(): RedisSentinelAssertion {
            val server = context?.let { RedisStore.server(it) }!!
            assertThat(server).isInstanceOf(RedisCluster::class.java)
            val cluster = server as RedisCluster
            assertThat(cluster.sentinels()).isNotNull()
            return RedisSentinelAssertion(cluster.sentinels())
        }
    }

    inner class RedisConfAssertion(val conf: List<RedisConf>) {

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
            assertThat(conf.flatMap { it.directives }.toList()).contains(directive)
            return this
        }
    }

    inner class RedisSentinelAssertion(private val sentinels: List<redis.embedded.Redis>) {

        val sentinelsAndConf = sentinels.map { it to RedisConfParser.parse(RedisConfLocator.locate(it)) }

        fun and(): RedisSentinelAssertion {
            return this
        }

        fun andAlso(): RedisTests {
            return this@RedisTests
        }

        fun withOne(): RedisSentinelAssertion {
            return this
        }

        fun withAtLeastOne(): RedisSentinelAssertion {
            return this
        }

        fun thatHaveASizeOf(size: Int): RedisSentinelAssertion {
            assertThat(sentinels).hasSize(size)
            return this
        }

        fun thatRunsOn(bind: String, port: Int): RedisSentinelAssertion {
            RedisConfAssertion(sentinelsAndConf.filter { it.first.ports().contains(port) }.map { it.second })
                .thatContainsDirective("bind", bind)
            return this
        }

        fun thatMonitors(
            group: String,
            bind: String,
            port: Int,
            quorumSize: Int = 1
        ): RedisSentinelAssertion {
            RedisConfAssertion(sentinelsAndConf.map { it.second })
                .thatContainsDirective("sentinel", "monitor", group, bind, port.toString(), quorumSize.toString())
            return this
        }

        fun thatHasDownAfterMillis(group: String, downAfterMillis: Long): RedisSentinelAssertion {
            RedisConfAssertion(sentinelsAndConf.map { it.second })
                .thatContainsDirective("sentinel", "down-after-milliseconds", group, downAfterMillis.toString())
            return this
        }

        fun thatHasFailOverTimeoutMillis(group: String, failOverTimeoutMillis: Long): RedisSentinelAssertion {
            RedisConfAssertion(sentinelsAndConf.map { it.second })
                .thatContainsDirective("sentinel", "failover-timeout", group, failOverTimeoutMillis.toString())
            return this
        }

        fun thatHasParallelSyncs(group: String, parallelSyncs: Int): RedisSentinelAssertion {
            RedisConfAssertion(sentinelsAndConf.map { it.second })
                .thatContainsDirective("sentinel", "parallel-syncs", group, parallelSyncs.toString())
            return this
        }
    }
}