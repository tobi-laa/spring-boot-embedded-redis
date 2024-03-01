package io.github.tobi.laa.spring.boot.embedded.redis.standalone

import io.github.tobi.laa.spring.boot.embedded.redis.junit.extension.EmbeddedRedisTest
import org.springframework.test.context.ContextCustomizerFactories
import kotlin.reflect.KClass

/**
 * Annotation to enable a standalone [embedded Redis server][redis.embedded.RedisServer] for tests.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@EmbeddedRedisTest
@ContextCustomizerFactories(RedisStandaloneContextCustomizerFactory::class)
annotation class EmbeddedRedisStandalone(

    /**
     * The port to start the embedded Redis server on. If set to 0, a free port upwards from `6379` will be used.
     * @see redis.embedded.core.RedisServerBuilder.port
     */
    val port: Int = 0,

    /**
     * The path to the Redis config file to use. If set, the config will be loaded from the file.
     *
     * > **Warning**
     * Due to how embedded Redis is implemented, a [port] specified in the `redis.conf` file will be ignored.
     *
     * > **Warning**
     * Cannot be set together with [settings].
     *
     * @see redis.embedded.core.RedisServerBuilder.configFile
     */
    val configFile: String = "",

    /**
     * The bind address to use. If set, the server will only bind to the given address.
     * @see redis.embedded.core.RedisServerBuilder.bind
     */
    val bind: String = "localhost",

    /**
     * The setting to use. If set, the server will be started with the given settings.
     *
     * > **Warning**
     * Cannot be set together with [configFile].
     *
     * @see redis.embedded.core.RedisServerBuilder.setting
     */
    val settings: Array<String> = [],

    /**
     * The path to the directory to execute the Redis server in. If set, the Redis executable will be executed in the
     * given directory.
     * @see redis.embedded.core.ExecutableProvider.newJarResourceProvider
     */
    val executeInDirectory: String = "",

    /**
     * Customizes how the Redis server is built. Customizers are ordered by their natural order in this array. Each
     * customizer must have no-arg constructor.
     * @see RedisStandaloneCustomizer
     */
    val customizer: Array<KClass<out RedisStandaloneCustomizer>> = []
)