package io.github.tobi.laa.spring.boot.embedded.redis.conf

import org.assertj.core.api.AbstractThrowableAssert
import org.assertj.core.api.Assertions.*
import org.assertj.core.api.ThrowableAssert.ThrowingCallable
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Named.named
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.ArgumentsProvider
import org.junit.jupiter.params.provider.ArgumentsSource
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream
import kotlin.io.path.listDirectoryEntries

@DisplayName("Tests for RedisConfParser")
internal class RedisConfParserTest {

    private var givenFile: Path? = null
    private var parse: ThrowingCallable? = null
    private var result: RedisConf? = null

    @DisplayName("Official self documented examples for Redis should be parsed without error")
    @ParameterizedTest(name = "{0} should be parsed without error")
    @ArgumentsSource(OfficialExamples::class)
    fun officialSelfDocumentedExample_whenParsing_shouldSucceed(file: Path) {
        givenFile(file)
        whenParsing()
        thenValidRedisConfShouldBeReturned()
    }

    @DisplayName("Invalid redis.conf should yield error when being parsed")
    @ParameterizedTest(name = "{0} should yield error message -> {1}")
    @ArgumentsSource(InvalidExamples::class)
    fun invalidExample_whenParsing_shouldYieldError(file: Path, expectedMsg: String) {
        givenFile(file)
        whenParsing()
        thenErrorShouldBeThrown().message().contains(expectedMsg)
    }

    @DisplayName("Valid redis.conf should be parsed correctly with expected directives")
    @ParameterizedTest(name = "{0} should be parsed without error and yield expected RedisConf")
    @ArgumentsSource(ValidExamples::class)
    fun validExample_whenParsing_shouldYieldExpectedConf(file: Path, expected: RedisConf) {
        givenFile(file)
        whenParsing()
        thenParsedConfShouldBeAs(expected)
    }

    private fun givenFile(file: Path) {
        givenFile = file
    }

    private fun whenParsing() {
        parse = ThrowingCallable { result = RedisConfParser.parse(givenFile!!) }
    }

    private fun thenValidRedisConfShouldBeReturned() {
        assertThatCode { parse!!.call() }.doesNotThrowAnyException()
        assertThat(result).isNotNull
        assertThat(result!!.directives).isNotEmpty
    }

    private fun thenParsedConfShouldBeAs(expected: RedisConf) {
        assertThatCode { parse!!.call() }.doesNotThrowAnyException()
        assertThat(result).usingRecursiveComparison().isEqualTo(expected)
    }

    private fun thenErrorShouldBeThrown(): AbstractThrowableAssert<*, *> {
        return assertThatThrownBy { parse!!.call() }.isExactlyInstanceOf(IllegalArgumentException::class.java)
    }

    internal class OfficialExamples : ArgumentsProvider {

        private val dir = Paths.get("src/test/resources/redis-conf/official-examples")

        override fun provideArguments(extensionContext: ExtensionContext?): Stream<Arguments> {
            val versions = dir.listDirectoryEntries().map { it.fileName }
            return versions
                .map {
                    arguments(
                        named(
                            "Example for version ${it}",
                            dir.resolve(it).resolve("redis.conf")
                        )
                    )
                }
                .stream()
        }
    }

    internal class InvalidExamples : ArgumentsProvider {

        private val dir = Paths.get("src/test/resources/redis-conf/invalid-examples")

        override fun provideArguments(extensionContext: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(
                    named(
                        "Conf with argument has unbalanced double quote",
                        dir.resolve("argument-unbalanced-double-quote.conf")
                    ),
                    "Unbalanced quotes in arguments: '6379\"'"
                ),
                arguments(
                    named(
                        "Conf with argument has unbalanced single quote",
                        dir.resolve("argument-unbalanced-single-quote.conf")
                    ),
                    "Unbalanced quotes in arguments: ''6379'"
                ),
                arguments(
                    named(
                        "Conf with keyword has double quotes",
                        dir.resolve("double-quoted-keyword.conf")
                    ),
                    "Keyword '\"port\"' contains illegal characters. Only alphanumeric characters, hyphens and underscores are allowed"
                ),
                arguments(
                    named(
                        "Conf with keyword has single quotes",
                        dir.resolve("single-quoted-keyword.conf")
                    ),
                    "Keyword ''port'' contains illegal characters. Only alphanumeric characters, hyphens and underscores are allowed"
                ),
                arguments(
                    named(
                        "Conf with keyword that contains hash",
                        dir.resolve("keyword-contains-hash.conf")
                    ),
                    "Keyword 'po#rt' contains illegal characters. Only alphanumeric characters, hyphens and underscores are allowed"
                ),
                arguments(
                    named(
                        "Conf with missing arguments",
                        dir.resolve("missing-arguments.conf")
                    ),
                    "No arguments found in line: 'port'"
                )
            )
        }
    }

    internal class ValidExamples : ArgumentsProvider {

        private val dir = Paths.get("src/test/resources/redis-conf/valid-examples")

        override fun provideArguments(extensionContext: ExtensionContext?): Stream<Arguments> {
            return Stream.of(
                arguments(
                    named(
                        "Empty config file",
                        dir.resolve("empty.conf")
                    ),
                    RedisConf(emptyList())
                ),
                arguments(
                    named(
                        "Conf with duplicate keywords",
                        dir.resolve("with-duplicate-keywords.conf")
                    ),
                    RedisConf(
                        listOf(
                            RedisConf.Directive("bind", "localhost"),
                            RedisConf.Directive("bind", "127.0.0.1", "::1"),
                            RedisConf.Directive("port", "6379")
                        )
                    )
                ),
                arguments(
                    named(
                        "Conf with escaped arguments",
                        dir.resolve("with-escaped-arguments.conf")
                    ),
                    RedisConf(
                        listOf(
                            RedisConf.Directive("tls-protocols", "TLSv1.2 TLSv1.3"),
                            RedisConf.Directive("logfile", ""),
                            RedisConf.Directive("proc-title-template", "{title} {listen-addr} {server-mode}")
                        )
                    )
                ),
                arguments(
                    named(
                        "Conf without duplicate keywords",
                        dir.resolve("without-duplicate-keywords.conf")
                    ),
                    RedisConf(
                        listOf(
                            RedisConf.Directive("bind", "i.like.trains.org"),
                            RedisConf.Directive("port", "6379")
                        )
                    )
                )
            )
        }
    }
}