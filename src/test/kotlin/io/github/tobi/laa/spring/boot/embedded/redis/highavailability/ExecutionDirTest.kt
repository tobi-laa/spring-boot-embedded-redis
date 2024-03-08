package io.github.tobi.laa.spring.boot.embedded.redis.highavailability

import io.github.tobi.laa.spring.boot.embedded.redis.IntegrationTest
import io.github.tobi.laa.spring.boot.embedded.redis.RedisTests
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.io.File

private const val CUSTOM_EXECUTION_DIR = "build/custom-execution-dir-high-availability"

@IntegrationTest
@EmbeddedRedisHighAvailability(executeInDirectory = CUSTOM_EXECUTION_DIR)
@DisplayName("Using @EmbeddedRedisHighAvailability with custom execution dir")
internal class ExecutionDirTest {

    @Autowired
    private lateinit var given: RedisTests

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            File(CUSTOM_EXECUTION_DIR).mkdirs()
        }
    }

    @Test
    @DisplayName("It should be possible to write to Redis and the data should be available afterwards")
    fun givenRandomTestdata_writingToRedis_dataShouldBeAvailable() {
        given.randomTestdata()
            .whenRedis().isBeingWrittenTo()
            .then().redis().shouldContainTheTestdata()
    }
}