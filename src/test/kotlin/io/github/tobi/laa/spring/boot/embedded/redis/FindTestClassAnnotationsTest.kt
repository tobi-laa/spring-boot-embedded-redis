package io.github.tobi.laa.spring.boot.embedded.redis

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Tests for findTestClassAnnotations()")
internal class FindTestClassAnnotationsTest {

    @Test
    @DisplayName("findTestClassAnnotations() should return empty list if class has no annotations")
    fun noAnnotations_findTestClassAnnotations_shouldReturnEmptyList() {
        val actualTopLevel =
            findTestClassAnnotations(TopLevelClassNoAnnotations::class.java, TestAnnotation::class.java)
        val actualNested =
            findTestClassAnnotations(TopLevelClassNoAnnotations.NestedClass::class.java, TestAnnotation::class.java)
        assertThat(actualTopLevel).isEmpty()
        assertThat(actualNested).isEmpty()
    }

    @Test
    @DisplayName("findTestClassAnnotations() should return the expected annotation if annotated directly")
    fun directAnnotations_findTestClassAnnotations_shouldReturnDirectAnnotations() {
        val actualTopLevel =
            findTestClassAnnotations(TopLevelClassDirectlyAnnotated::class.java, TestAnnotation::class.java)
        val actualNested =
            findTestClassAnnotations(
                TopLevelClassDirectlyAnnotated.NestedClassDirectlyAnnotated::class.java,
                TestAnnotation::class.java
            )
        assertThat(actualTopLevel).hasSize(1).element(0).satisfies(
            { it: TestAnnotation ->
                assertThat(it.foo).isEqualTo("foo")
                assertThat(it.bar).isEqualTo(42)

            })
        assertThat(actualNested).hasSize(1).element(0).satisfies(
            { it: TestAnnotation ->
                assertThat(it.foo).isEqualTo("foo")
                assertThat(it.bar).isEqualTo(42)

            })
    }

    @Test
    @DisplayName("findTestClassAnnotations() should return the expected annotation if annotated with compound annotation")
    fun compoundAnnotations_findTestClassAnnotations_shouldReturnCompoundAnnotations() {
        val actualTopLevel =
            findTestClassAnnotations(TopLevelClassCompoundAnnotation::class.java, TestAnnotation::class.java)
        val actualNested =
            findTestClassAnnotations(
                TopLevelClassCompoundAnnotation.NestedClassCompoundAnnotation::class.java,
                TestAnnotation::class.java
            )
        assertThat(actualTopLevel).hasSize(1).element(0).satisfies(
            { it: TestAnnotation ->
                assertThat(it.foo).isEqualTo("baz")
                assertThat(it.bar).isEqualTo(23)

            })
        assertThat(actualNested).hasSize(1).element(0).satisfies(
            { it: TestAnnotation ->
                assertThat(it.foo).isEqualTo("baz")
                assertThat(it.bar).isEqualTo(23)

            })
    }

    @Test
    @DisplayName("findTestClassAnnotations() should return the expected annotation if annotated with both direct and compound annotations")
    fun bothAnnotations_findTestClassAnnotations_shouldReturnBothAnnotations() {
        val actualTopLevel =
            findTestClassAnnotations(TopLevelClassBoth::class.java, TestAnnotation::class.java)
        val actualNested =
            findTestClassAnnotations(
                TopLevelClassBoth.NestedClassBothAndMore::class.java,
                TestAnnotation::class.java
            )
        assertThat(actualTopLevel)
            .hasSize(2)
            .anySatisfy { it: TestAnnotation ->
                assertThat(it.foo).isEqualTo("foo")
                assertThat(it.bar).isEqualTo(42)
            }
            .anySatisfy { it: TestAnnotation ->
                assertThat(it.foo).isEqualTo("baz")
                assertThat(it.bar).isEqualTo(23)

            }
        assertThat(actualNested)
            .hasSize(3)
            .anySatisfy { it: TestAnnotation ->
                assertThat(it.foo).isEqualTo("foo")
                assertThat(it.bar).isEqualTo(42)
            }
            .anySatisfy { it: TestAnnotation ->
                assertThat(it.foo).isEqualTo("baz")
                assertThat(it.bar).isEqualTo(23)

            }
            .anySatisfy { it: TestAnnotation ->
                assertThat(it.foo).isEqualTo("bar")
                assertThat(it.bar).isEqualTo(55)

            }
    }

    @TestAnnotation
    internal class TopLevelClassDirectlyAnnotated {

        @Nested
        internal inner class NestedClassDirectlyAnnotated
    }

    @CompoundAnnotation
    internal class TopLevelClassCompoundAnnotation {

        @Nested
        internal inner class NestedClassCompoundAnnotation
    }

    @TestAnnotation
    @CompoundAnnotation
    internal class TopLevelClassBoth {

        @Nested
        @TestAnnotation("bar", 55)
        internal inner class NestedClassBothAndMore
    }

    internal class TopLevelClassNoAnnotations {

        @Nested
        internal inner class NestedClass
    }

    @Target(AnnotationTarget.CLASS)
    @Retention(AnnotationRetention.RUNTIME)
    internal annotation class TestAnnotation(
        val foo: String = "foo",
        val bar: Int = 42
    )

    @TestAnnotation("baz", 23)
    internal annotation class CompoundAnnotation
}