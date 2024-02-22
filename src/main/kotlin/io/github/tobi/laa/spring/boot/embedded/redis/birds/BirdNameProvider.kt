package io.github.tobi.laa.spring.boot.embedded.redis.birds

/**
 * Provides common english names of random birds. Used for naming shards of the Redis cluster.
 */
internal object BirdNameProvider {

    private val birds = javaClass.getResource("/birds.txt")!!.readText().lines().shuffled().iterator()

    internal fun next() = birds.next()
}