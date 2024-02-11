package io.github.tobi.laa.spring.boot.embedded.redis

/**
 * Can be used to specify when to call [`FLUSHALL`](https://redis.io/commands/flushall/) on the embedded Redis server,
 * i.e. when to clear all data.
 *
 * By default, `FLUSHALL` is called after each test method. This is also the case when the annotation is not present.
 */
annotation class RedisFlushAll(
    /**
     * When to call `FLUSHALL` on the embedded Redis server.
     */
    val mode: Mode = Mode.AFTER_METHOD
) {
    
    /**
     * Whether to call `FLUSHALL` after each test method, after each test class or never.
     */
    enum class Mode {
        /**
         * Call `FLUSHALL` after each test method.
         * @see org.junit.jupiter.api.AfterEach
         */
        AFTER_METHOD,

        /**
         * Call `FLUSHALL` after each test class.
         * @see org.junit.jupiter.api.AfterAll
         */
        AFTER_CLASS,

        /**
         * Never call `FLUSHALL`.
         */
        @Suppress("unused")
        NEVER
    }
}
