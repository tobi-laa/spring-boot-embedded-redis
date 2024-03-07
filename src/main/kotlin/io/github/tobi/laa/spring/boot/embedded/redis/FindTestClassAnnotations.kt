package io.github.tobi.laa.spring.boot.embedded.redis

import org.springframework.core.annotation.MergedAnnotations.SearchStrategy.TYPE_HIERARCHY
import org.springframework.core.annotation.MergedAnnotations.search
import org.springframework.test.context.TestContextAnnotationUtils.searchEnclosingClass

internal fun <T : Annotation> findTestClassAnnotations(
    testClass: Class<*>,
    annotationType: Class<T>
): List<T> {
    var clazz = testClass
    val mergedAnnotations = mutableListOf(search(TYPE_HIERARCHY).from(testClass))
    while (searchEnclosingClass(clazz)) {
        clazz = clazz.enclosingClass
        mergedAnnotations += search(TYPE_HIERARCHY).from(clazz)
    }
    return mergedAnnotations
        .stream()
        .flatMap { it.stream(annotationType) }
        .map { it.synthesize() }
        .toList()
}