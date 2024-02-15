package io.github.tobi.laa.spring.boot.embedded.redis

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests
import com.tngtech.archunit.junit.AnalyzeClasses
import com.tngtech.archunit.junit.ArchTest
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields
import com.tngtech.archunit.library.GeneralCodingRules.*

/**
 * Some general coding rules, in broad parts copied over from the ArchUnit examples.
 */
@Suppress("Unused")
@AnalyzeClasses(packagesOf = [CodingRulesTest::class], importOptions = [DoNotIncludeTests::class])
internal class CodingRulesTest {

    @ArchTest
    private val `No classes should access standard streams` = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS

    @ArchTest
    private val `No classes should throw generic exceptions` = NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS

    @ArchTest
    private val `No classes should use Java 2 platform's core logging facilities` =
        NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING

    @ArchTest
    private val `Loggers should be private and static and final` =
        fields().that()
            .haveRawType("org.slf4j.Logger")
            .should().bePrivate()
            .andShould().beStatic()
            .andShould().beFinal()
            .`as`("Loggers should private static final.")
            .because("That is a convention for this project.")
            .allowEmptyShould(true)

    @ArchTest
    private val `No classes should use Joda time` = NO_CLASSES_SHOULD_USE_JODATIME

    @ArchTest
    private val `No classes should use field injection` = NO_CLASSES_SHOULD_USE_FIELD_INJECTION
}