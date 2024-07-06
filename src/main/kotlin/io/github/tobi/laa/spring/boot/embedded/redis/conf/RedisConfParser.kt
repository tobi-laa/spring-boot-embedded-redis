package io.github.tobi.laa.spring.boot.embedded.redis.conf

import io.github.tobi.laa.spring.boot.embedded.redis.conf.RedisConfParser.ArgsParseState.*
import java.nio.file.Path
import kotlin.io.path.readLines

/**
 * Parses a `redis.conf` file.
 */
internal object RedisConfParser {

    /**
     * Parses the given `redis.conf` file.
     * @param file the `redis.conf` file to parse
     * @return the parsed [RedisConf] file
     * @throws IllegalArgumentException if the file is not a valid `redis.conf` file
     */
    internal fun parse(file: Path): RedisConf {
        val directives = file
            .readLines()
            .map { it.trim() }
            .filterNot { it.isBlank() }
            .filterNot { isComment(it) }
            .map { parseDirective(it) }
        return RedisConf(directives)
    }

    private fun isComment(line: String): Boolean {
        return line.startsWith("#")
    }

    private fun parseDirective(line: String): RedisConf.Directive {
        line.split(Regex("\\s+"), 2).let {
            val keyword = it[0]
            val arguments = it.getOrNull(1) ?: throw IllegalArgumentException("No arguments found in line: '$line'")
            return RedisConf.Directive(keyword, parseArguments(arguments))
        }
    }

    private fun parseArguments(rawArguments: String): List<String> {
        val arguments = mutableListOf<String>()
        var state = UNESCAPED
        var currentArg = ""
        for (char in rawArguments) {
            when {
                char == '"' && state == UNESCAPED -> {
                    state = ESCAPED_DOUBLE
                }

                char == '\'' && state == UNESCAPED -> {
                    state = ESCAPED_SINGLE
                }

                char == '"' && state == ESCAPED_DOUBLE -> {
                    state = UNESCAPED
                }

                char == '\'' && state == ESCAPED_SINGLE -> {
                    state = UNESCAPED
                }

                char == ' ' && state == UNESCAPED -> {
                    arguments.add(currentArg)
                    currentArg = ""
                }

                else -> {
                    currentArg += char
                }
            }
        }
        when {
            state != UNESCAPED -> {
                throw IllegalArgumentException("Unbalanced quotes in arguments: '$rawArguments'")
            }

            else -> {
                arguments.add(currentArg)
                return arguments
            }
        }
    }

    internal enum class ArgsParseState {
        UNESCAPED, ESCAPED_SINGLE, ESCAPED_DOUBLE
    }
}