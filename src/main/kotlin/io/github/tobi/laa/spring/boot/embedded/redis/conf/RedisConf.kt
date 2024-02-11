package io.github.tobi.laa.spring.boot.embedded.redis.conf

private const val KEYWORD_PORT = "port"
private const val KEYWORD_BIND = "bind"

/**
 * Representation of a `redis.conf` file, i.e. the configuration of a Redis server.
 */
internal data class RedisConf(val directives: List<Directive>) {

    fun getPorts(): List<Int> {
        return getDirectives(KEYWORD_PORT).map { it.arguments.first().toInt() }
    }

    fun getBinds(): List<String> {
        return getDirectives(KEYWORD_BIND).map { it.arguments.first() }
    }

    fun getDirectives(keyword: String): List<Directive> {
        return directives.filter { it.keyword == keyword }
    }

    internal data class Directive(val keyword: String, val arguments: List<String>) {

        constructor(keyword: String, vararg arguments: String) : this(keyword, arguments.asList())

        init {
            require(keyword.isNotBlank()) { "Keyword must not be blank" }
            require(keyword.matches(Regex("[a-zA-Z0-9_-]+"))) {
                "Keyword '${keyword}' contains illegal characters. Only alphanumeric characters, hyphens and underscores are allowed"
            }
            require(arguments.isNotEmpty()) { "At least one argument is required" }
        }
    }
}