package io.github.tobi.laa.spring.boot.embedded.redis.birds

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Tests for BirdNameProvider")
internal class BirdNameProviderTest {

    @Test
    @DisplayName("Calling next() should return a valid bird name")
    fun requestingBirdName_shouldReturnValidBirdName() {
        val birdName = BirdNameProvider.next()
        assertThat(birdName).isNotBlank()
    }
}